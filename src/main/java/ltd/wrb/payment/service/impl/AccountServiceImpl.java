package ltd.wrb.payment.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;

import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.enums.TradeType;
import ltd.wrb.payment.model.EVMChainConfig;
import ltd.wrb.payment.model.TradeLog;
import ltd.wrb.payment.repository.TradeLogRepository;
import ltd.wrb.payment.service.AccountService;
import ltd.wrb.payment.service.AccountServiceException;
import ltd.wrb.payment.service.telegram.Telegram;
import ltd.wrb.payment.util.RedisUtils;
import ltd.wrb.payment.util.Web3jUtil;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    @Autowired
    private RedisUtils cache;

    @Autowired
    private TradeLogRepository tradeLogRepository;

    @Autowired(required = false)
    private Telegram telegram;

    private ExecutorService executor = Executors.newFixedThreadPool(20);

    @Override
    public BigDecimal getBalance(String userid) {
        Number n = tradeLogRepository.sumAmountByUid(userid);
        return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString());
    }

    @Override
    public void addTradeLog(TradeLog tradeLog) throws AccountServiceException {
        // tradeLog.setId(
        // IdUtil.getSnowflake(2, 1).nextIdStr());
        tradeLog.setCreatedAt(System.currentTimeMillis());

        if (tradeLog.getTxHash() != null && !"".equals(tradeLog.getTxHash())) {
            String hashLock = "trade_log_" + tradeLog.getTxHash() + "_" + tradeLog.getType();
            if (!cache.lock(hashLock, 3600)) {
                throw new AccountServiceException("Trade log already in process");
            }

            if (tradeLogRepository.existsByTxHash(tradeLog.getTxHash())) {
                cache.unlock(hashLock);
                throw new AccountServiceException("Trade log already exists");
            }
        }

        // amount < 0
        try {
            if (tradeLog.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                String lock = "trade_log_balance_" + tradeLog.getUid();
                if (!cache.lock(lock, 60)) {
                    throw new AccountServiceException("Trade log already in process (balance)");
                }
                try {
                    BigDecimal balance = getBalance(tradeLog.getUid());
                    if (balance.compareTo(tradeLog.getAmount().negate()) < 0) {
                        throw new AccountServiceException("Insufficient balance");
                    }
                    tradeLogRepository.save(tradeLog);
                } finally {
                    cache.unlock(lock);
                }
            } else {
                tradeLogRepository.save(tradeLog);
            }
        } catch (Exception e) {
            log.error("Error processing trade log: {}", e.getMessage(), e);
            throw e;
        }
        log.info("[TradeLog] {}", tradeLog);

        if (tradeLog.getType() == TradeType.DEPOSIT && telegram != null) {
            executor.execute(() -> {
                String chatId = System.getenv("TELEGRAM_ADMIN_GROUP_CHAT_ID");
                log.info("[Telegram] {}", chatId);
                try {
                    if (chatId != null && !chatId.isEmpty()) {
                        SendMessage sendMessage = new SendMessage(Long.parseLong(chatId),
                                "<b>Deposit: </b><code>" + tradeLog.getAmount() + "</code> USDT \n"
                                        + "<b>Hash:</b> <code>" + tradeLog.getTxHash() + "</code> \n"
                                        + "<b>From:</b> <code>" + tradeLog.getTxFrom() + "</code> \n"
                                        + "<b>To user: </b>" + tradeLog.getUid() + "\n"
                                        + "<b>To address: </b>" + tradeLog.getTxTo() + "\n"
                                        + "<b>Memo: </b>" + tradeLog.getMemo() + " \n"
                                        + "<b>Block:</b> " + tradeLog.getBlockNumber() + "\n"
                                        + "<b>UTD Time: </b>"
                                        + LocalDateTime.ofEpochSecond(tradeLog.getCreatedAt() / 1000, 0,
                                                java.time.ZoneOffset.UTC)
                                        + "\n"
                                        + "<b>Chain: </b>" + tradeLog.getChainType() + "\n"
                                        + "<b>Chain Id: </b>" + tradeLog.getChainId() + "\n");
                        sendMessage.protectContent(true);
                        sendMessage.parseMode(ParseMode.HTML);
                        telegram.sendMessage(sendMessage);
                        log.info("[Telegram] {}", sendMessage);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public boolean isTradeLogExist(String txHash) {
        return tradeLogRepository.existsByTxHash(txHash);
    }

    @Override
    public TradeLog withdrawApply(String uid, BigDecimal amount, String to, ChainType chainType, Integer chainId,
            String token) throws AccountServiceException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountServiceException("Invalid amount");
        }
        String lock = "withdraw_" + uid;
        if (!cache.lock(lock, 60)) {
            throw new AccountServiceException("Withdraw already in process");
        }

        // check balance
        BigDecimal balance = getBalance(uid);
        if (balance.compareTo(amount) < 0) {
            cache.unlock(lock);
            throw new AccountServiceException("Insufficient balance");
        }

        // TradeLog
        TradeLog tradeLog = TradeLog.builder().uid(uid)
                .amount(amount.negate())
                .memo("Withdraw")
                .chainId(chainId)
                .token(token)
                .chainType(chainType)
                .type(TradeType.WITHDRAW_APPLY)
                .createdAt(System.currentTimeMillis())
                .txTo(to)
                .build();
        addTradeLog(tradeLog);
        cache.unlock(lock);
        return tradeLog;
    }

    @Override
    public void withdrawConfirm(Long logId, String txHash, BigInteger blockNumber) throws AccountServiceException {
        TradeLog tradeLog = tradeLogRepository.findById(logId).orElse(null);
        if (tradeLog == null) {
            throw new AccountServiceException("Trade log not found");
        }
        if (tradeLog.getType() != TradeType.WITHDRAW_APPLY) {
            throw new AccountServiceException("Invalid trade log type");
        }
        if (tradeLog.getTxHash() != null && !tradeLog.getTxHash().isEmpty()) {
            throw new AccountServiceException("Trade log already confirmed");
        }
        // get the transaction from the chain and check the transaction status

        if (!checkTxRecept(tradeLog)) {
            log.error("Transaction not confirmed: {}", txHash);
        } else {
            tradeLog.setType(TradeType.WITHDRAW_CONFIRM);
        }

        tradeLog.setTxHash(txHash);
        tradeLog.setBlockNumber(blockNumber.toString());
        tradeLogRepository.save(tradeLog);
    }

    private boolean checkTxRecept(TradeLog tradeLog) {
        if (tradeLog.getChainType() == ChainType.TRON) {
            return checkTronTxRecept(tradeLog.getTxHash());
        } else if (tradeLog.getChainType() == ChainType.EVM) {
            return checkEvmTxRecept(tradeLog.getChainId(), tradeLog.getTxHash());
        }
        return false;
    }

    private boolean checkTronTxRecept(String txHash) {
        try {
            Web3j web3j = Web3jUtil.getWeb3(System.getenv("TRON_RPC_URL"));
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send().getResult();
            return receipt != null && receipt.isStatusOK();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean checkEvmTxRecept(Integer chainId, String txHash) {
        EVMChainConfig chainConfig = EVMChainConfig.getChainConfig(chainId);
        if (chainConfig == null) {
            return false;
        }
        try {
            Web3j web3j = Web3jUtil.getWeb3(chainConfig.getRpc());
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send().getResult();
            return receipt != null && receipt.isStatusOK();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void rejectWithdraw(Long logId) throws AccountServiceException {
        TradeLog tradeLog = tradeLogRepository.findById(logId).orElse(null);
        if (tradeLog == null) {
            throw new AccountServiceException("Trade log not found");
        }
        if (tradeLog.getType() != TradeType.WITHDRAW_APPLY) {
            throw new AccountServiceException("Invalid trade log type");
        }
        if (tradeLog.getTxHash() != null && !tradeLog.getTxHash().isEmpty()) {
            throw new AccountServiceException("Trade log already confirmed");
        }
        tradeLog.setType(TradeType.WITHDRAW_REJECT);
        tradeLogRepository.save(tradeLog);

        // refund to account
        TradeLog refundLog = TradeLog.builder().uid(tradeLog.getUid())
                .amount(tradeLog.getAmount().negate())
                .memo("Withdraw Refund")
                .chainId(tradeLog.getChainId())
                .token(tradeLog.getToken())
                .chainType(tradeLog.getChainType())
                .type(TradeType.WITHDRAW_REJECT)
                .createdAt(System.currentTimeMillis())
                .txTo(tradeLog.getTxTo())
                .build();
        addTradeLog(refundLog);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
