package ltd.wrb.payment.job.evm;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.web3j.abi.EventEncoder;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;

import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.contracts.IERC20Metadata;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.job.OnDeposit;
import ltd.wrb.payment.job.Scanner;
import ltd.wrb.payment.model.EVMChainConfig;
import ltd.wrb.payment.model.EVMChainConfigToken;
import ltd.wrb.payment.util.Web3jUtil;

@Slf4j
public class EVMScanner implements Scanner {

    private final EVMChainConfig config;

    private Web3j web3j;

    private Disposable flow;

    private final OnDeposit onDeposit;

    public EVMScanner(EVMChainConfig config, OnDeposit onDeposit) {
        this.config = config;
        this.onDeposit = onDeposit;
    }

    public void init() {
        try {
            web3j = Web3jUtil.getWeb3(config.getRpc());
        } catch (ConnectException e) {
            log.error("Failed to connect to EVM chain: {}", config.getRpc(), e);
            throw new RuntimeException("Failed to connect to EVM chain: " + config.getRpc());
        }
    }

    @Override
    public void restart() {
        init();
        start();
    }

    @Override
    public boolean isRunning() {
        return flow != null && !flow.isDisposed();
    }

    @Scheduled(fixedDelay = 60000)
    public void retryRPC() {
        if (flow == null || flow.isDisposed()) {
            log.info("Reconnecting to EVM chain: {}", config.getRpc());
            init();
            start();
        }
    }

    public void start() {

        try {
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            log.info("Current block number: {}", blockNumber);

            List<String> usdtAddress = config.getUsdtContracts().stream().map(EVMChainConfigToken::getAddress).toList();
            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(blockNumber.subtract(new BigInteger("100"))),
                    DefaultBlockParameterName.LATEST,
                    // config.getUsdtContract()
                    usdtAddress
                );
            filter.addSingleTopic(EventEncoder.encode(IERC20Metadata.TRANSFER_EVENT));

            flow = web3j.ethLogFlowable(filter).subscribe(event -> {
                // log.info("New log: {}", event);
                processEvent(event, false);
            }, error -> {
                log.error("Failed to get log flow: " + config.getChainName() , error);
            });

        } catch (IOException e) {
            log.error("Failed to get block number", e);
        }
    }

    public void processEvent(Log transferEvent, boolean checkBalance) {
        try {
            String to = transferEvent.getAddress();
            to = Keys.toChecksumAddress(to);
            if (!config.getUsdtContracts().stream().map(EVMChainConfigToken::getAddress).toList().contains(to)) {
                return;
            }
            // log the contract address and the transaction hash
            // log.info("Contract address: {}, tx: {}", to, transferEvent.getTransactionHash());
            
            // get to address
            IERC20Metadata.TransferEventResponse response = IERC20Metadata.getTransferEventFromLog(transferEvent);
            String toAddress = Keys.toChecksumAddress(response.to);
            String fromAddress = Keys.toChecksumAddress(response.from);
            BigInteger amount = response.value;

            final String _to = to;
            Integer decimals = config.getUsdtContracts().stream()
                    .filter(token -> token.getAddress().equalsIgnoreCase(_to))
                    .findFirst()
                    .map(EVMChainConfigToken::getDecimals)
                    .orElse(18);
            BigDecimal decimalsDivide = new BigDecimal("10").pow(decimals);
            BigDecimal amountDecimal = new BigDecimal(amount).divide(decimalsDivide, 6,
                    RoundingMode.CEILING);
            // check if to address is in wallet
            if (onDeposit != null && !onDeposit.isWalletExist(toAddress, ChainType.EVM)) {
                // log.error("Wallet not found: {}", toAddress);
                return;
            }
            // check the usdt balance of the to address
            if (checkBalance) {
                BigInteger usdtBalance = Web3jUtil.getErc20Balance(web3j, to, toAddress,
                        config.getChainId());
                log.info("USDT balance: {}", usdtBalance);

                if (usdtBalance.compareTo(amount) < 0) {
                    log.error("Insufficient USDT balance: {}", toAddress);
                    return;
                }
            }
            if (onDeposit != null)
                onDeposit.onDeposit(transferEvent.getTransactionHash(), fromAddress, toAddress, amountDecimal,
                        to,
                        ChainType.EVM,
                        config.getChainId(),
                        transferEvent.getBlockNumber());

        } catch (Exception e) {
            log.error("Failed to get transaction receipt: {}", transferEvent.getTransactionHash(), e);
        }
    }

    public void processBlocks(BigInteger startBlock, BigInteger endBlock) {
        try {
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            log.info("[process] Current block number: {}", blockNumber);

            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(startBlock),
                    DefaultBlockParameter.valueOf(endBlock),
                    config.getUsdtContracts().stream().map(EVMChainConfigToken::getAddress).toList()
            );
            filter.addSingleTopic(EventEncoder.encode(IERC20Metadata.TRANSFER_EVENT));

            web3j.ethLogFlowable(filter).subscribe(event -> {
                log.info("New log: {}", event);
                processEvent(event, false);
            }, error -> {
                log.error("Failed to get log flow", error);
            });

        } catch (IOException e) {
            log.error("Failed to get block number", e);
        }
    }
}
