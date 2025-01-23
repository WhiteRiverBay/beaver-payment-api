package ltd.wrb.payment.job.trx;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.ConnectException;

import org.springframework.scheduling.annotation.Scheduled;
import org.web3j.abi.EventEncoder;
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
import ltd.wrb.payment.util.TRONUtil;
import ltd.wrb.payment.util.Web3jUtil;

@Slf4j
public class TronScanner implements Scanner {

    private Web3j web3j;

    private Disposable flow;

    private final OnDeposit onDeposit;

    public TronScanner(OnDeposit onDeposit) {
        this.onDeposit = onDeposit;
    }

    public void init() {

        String rpc = getRPC();
        try {
            web3j = Web3jUtil.getWeb3(rpc);
        } catch (ConnectException e) {
            log.error("Failed to connect to Tron RPC: {}", rpc);
            throw new RuntimeException(e);
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

    private String getRPC() {
        return System.getenv("TRON_RPC_URL");
    }

    @Scheduled(fixedDelay = 60000)
    public void retryRPC() {
        if (flow == null || flow.isDisposed()) {
            log.info("Reconnecting to TRON chain: {}", getRPC());
            init();
            start();
        }
    }

    public void start() {
        try {
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            log.info("Current block number: {}", blockNumber);
            log.info("start watch: {}", TRONUtil.toETHAddress(System.getenv("TRON_USDT_CONTRACT")));

            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(blockNumber.subtract(new BigInteger("10"))),
                    DefaultBlockParameterName.LATEST,
                    TRONUtil.toETHAddress(System.getenv("TRON_USDT_CONTRACT")));
            filter.addSingleTopic(EventEncoder.encode(IERC20Metadata.TRANSFER_EVENT));

            flow = web3j.ethLogFlowable(filter).subscribe(event -> {
                // log.info("New log: {}", event);
                processEvent(event, false);
            }, error -> {
                log.error("Failed to get log flow", error);
            });

        } catch (IOException e) {
            log.error("Failed to get block number", e);
        }
    }

    public void processEvent(Log transferEvent, boolean checkBalance) {
        try {
            String to = TRONUtil.toTronAddress(transferEvent.getAddress());
            if (!to.equalsIgnoreCase(System.getenv("TRON_USDT_CONTRACT"))) {
                log.error("Invalid contract address: {}, expect {}", to, System.getenv("TRON_USDT_CONTRACT"));
                return;
            }

            // get to address
            IERC20Metadata.TransferEventResponse response = IERC20Metadata.getTransferEventFromLog(transferEvent);

            String toAddress = TRONUtil.toTronAddress(response.to);
            String fromAddress = TRONUtil.toTronAddress(response.from);
            BigInteger amount = response.value;
            BigDecimal amountDecimal = new BigDecimal(amount).divide(new BigDecimal("1000000"), 6,
                    RoundingMode.CEILING);
            if (isDebug()) {
                log.info("#{} {} transfer {} USDT to {} - hash: {}", transferEvent.getBlockNumber(), fromAddress,
                        amountDecimal, toAddress,
                        transferEvent.getTransactionHash());
            }
            // check if to address is in wallet
            if (onDeposit != null && !onDeposit.isWalletExist(toAddress, ChainType.TRON)) {
                if (isDebug()) {
                    log.error("Wallet not found: {}", toAddress);
                }
                return;
            }
            // check the usdt balance of the to address
            if (checkBalance) {
                BigInteger usdtBalance = Web3jUtil.getErc20Balance(web3j, toAddress,
                        System.getenv("TRON_USDT_CONTRACT"),
                        0x94a9059e);
                log.info("USDT balance: {}", usdtBalance);

                if (usdtBalance.compareTo(amount) < 0) {
                    log.error("Insufficient USDT balance: {}", toAddress);
                    return;
                }
            }

            if (onDeposit != null) {
                onDeposit.onDeposit(transferEvent.getTransactionHash(),
                        fromAddress, toAddress, amountDecimal,
                        to,
                        ChainType.TRON,
                        1000,
                        transferEvent.getBlockNumber());
            } else {
                log.error("onDeposit is null");
            }

        } catch (Exception e) {
            log.error("Failed to get transaction receipt: {}", transferEvent.getTransactionHash(), e);
        }
    }

    public void processBlocks(BigInteger startBlock, BigInteger endBlock) {
        try {
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            log.info("[process] Current block number: {}", blockNumber);
            log.info("[process] start watch: {}", TRONUtil.toETHAddress(System.getenv("TRON_USDT_CONTRACT")));

            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(startBlock),
                    DefaultBlockParameter.valueOf(endBlock),
                    TRONUtil.toETHAddress(System.getenv("TRON_USDT_CONTRACT")));
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

    public static void main(String[] args) {
        TronScanner scanner = new TronScanner(null);
        scanner.init();
        scanner.start();
    }

    private boolean isDebug() {
        return Boolean.parseBoolean(System.getenv("DEBUG"));
    }
}
