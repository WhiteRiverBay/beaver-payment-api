package ltd.wrb.payment.job;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.enums.TradeType;
import ltd.wrb.payment.model.EVMChainConfig;
import ltd.wrb.payment.model.TradeLog;
import ltd.wrb.payment.model.Wallet;
import ltd.wrb.payment.service.AccountService;
import ltd.wrb.payment.service.AccountServiceException;
import ltd.wrb.payment.service.PaymentService;
import ltd.wrb.payment.service.WalletService;
import ltd.wrb.payment.util.RedisUtils;

@Service
@Slf4j
public class OnDepositImpl implements OnDeposit {

    @Autowired
    private WalletService walletService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisUtils cache;

    @Override
    public boolean isWalletExist(String address, ChainType chainType) {
        return walletService.getByAddressAndChainType(address, chainType) != null;
    }

    @Override
    @Transactional
    public void onDeposit(String txHash, String from, String to, BigDecimal amount, String token, ChainType chainType,
            Integer chainId,
            BigInteger blockNumber, boolean autoPayLastOrder) {

        String hashLock = "trade_log_" + txHash;
        if (!cache.lock(hashLock, 3600)) {
            throw new RuntimeException("Trade log already in process, please retry after a while");
        }

        Wallet wallet = walletService.getByAddressAndChainType(to, chainType);
        if (wallet == null) {
            log.error("[OnDeposit] Wallet not found: {} {}", token, chainType);
            return;
        }
        log.info("[OnDeposit] {} {} {} {} {} {}", txHash, from, to, amount, token, chainType);
        // check if the trade log already exists, if so, return
        if (accountService.isTradeLogExist(txHash)) {
            log.error("[OnDeposit] Trade log already exists: {}", txHash);
            return;
        }

        EVMChainConfig chainConfig = EVMChainConfig.getChainConfig(chainId);
        int confirmBlockRequired = chainConfig != null ? chainConfig.getConfirmBlock() : 0;
        double threshold = chainConfig != null ? chainConfig.getConfirmThresholdAmountInDecimals().doubleValue() : 1000;
        if (amount.compareTo(new BigDecimal(threshold)) < 0) {
            confirmBlockRequired = 0;
        }

        // 处理入账、订单支付等逻辑
        TradeLog tradeLog = TradeLog.builder().txHash(txHash).txFrom(from).txTo(to).amount(amount).token(token)
                .uid(wallet.getUid())
                .confirmedBlocks(0)
                .confirmedBlocksRequired(confirmBlockRequired)
                .type(TradeType.DEPOSIT)
                .memo("Deposit")
                .chainId(chainId)
                .chainType(chainType).blockNumber(blockNumber.toString()).build();
        try {
            accountService.addTradeLog(tradeLog);
            if (autoPayLastOrder) {
                // 处理入账成功逻辑 - 找到最新的一个订单，检查是否能pay了
                paymentService.tryPayLastOrder(tradeLog.getUid(), true);
            }
        } catch (AccountServiceException e) {
            // 处理入账失败逻辑
            log.error("[OnDeposit] Failed to add trade log: {}", tradeLog, e);
        }

    }

    @Override
    public void onDeposit(String txHash, String from, String to, BigDecimal amount, String token, ChainType chainType,
            Integer chainId,
            BigInteger blockNumber) {
        onDeposit(txHash, from, to, amount, token, chainType, chainId, blockNumber, true);
    }

}
