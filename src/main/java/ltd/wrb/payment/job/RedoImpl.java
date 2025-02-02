package ltd.wrb.payment.job;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.contracts.IERC20Metadata;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.model.EVMChainConfig;
import ltd.wrb.payment.util.TRONUtil;
import ltd.wrb.payment.util.Web3jUtil;

@Component
@Slf4j
public class RedoImpl implements Redo {

    @Autowired
    private OnDeposit onDeposit;

    @Override
    public void checkTron(String hash) {
        String rpc = System.getenv("TRON_RPC_URL");
        check(rpc, hash, 1000, ChainType.TRON);
    }

    @Override
    public void checkEth(String hash, Integer chainId) {
        EVMChainConfig config = EVMChainConfig.load().get(chainId);
        if (config == null) {
            throw new RuntimeException("Chain not found: " + chainId);
        }
        check(config.getRpc(), hash, chainId, ChainType.EVM);
    }

    private void check(String rpc, String hash, Integer chainId, ChainType chainType) {
        try {
            Web3j web3j = Web3jUtil.getWeb3(rpc);
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(hash).send().getResult();
            if (receipt == null) {
                throw new RuntimeException("Transaction not found: " + hash);
            }

            if (!receipt.isStatusOK()) {
                throw new RuntimeException("Transaction returns a fail status: " + hash);
            }

            String to = TRONUtil.toTronAddress(receipt.getTo());
            if (!to.equalsIgnoreCase(System.getenv("TRON_USDT_CONTRACT"))) {
                log.error("Invalid contract address: {}, expect {}", to, System.getenv("TRON_USDT_CONTRACT"));
                throw new RuntimeException("Invalid contract address: " + to);
            }

            // analyze the logs of the transaction, is it a trc20 transfer?
            if (receipt.getLogs().isEmpty()) {
                throw new RuntimeException("No logs found in transaction: " + hash);
            }
            Log transferEvent = receipt.getLogs().get(0);
            List<String> topics = transferEvent.getTopics();
            if (topics.size() < 3) {
                throw new RuntimeException("Invalid log topics: " + topics);
            }
            // get to address
            IERC20Metadata.TransferEventResponse response = IERC20Metadata.getTransferEventFromLog(transferEvent);

            String toAddress = TRONUtil.toTronAddress(response.to);
            String fromAddress = TRONUtil.toTronAddress(response.from);
            BigInteger amount = response.value;
            BigDecimal amountDecimal = new BigDecimal(amount).divide(new BigDecimal("1000000"), 6,
                    RoundingMode.CEILING);
            log.info("[REDO] #{} {} transfer {} USDT to {} - hash: {}", transferEvent.getBlockNumber(), fromAddress,
                    amountDecimal, toAddress,
                    transferEvent.getTransactionHash());

            // check if to address is in wallet
            if (onDeposit != null && !onDeposit.isWalletExist(toAddress, ChainType.TRON)) {
                log.error("Wallet not found: {}", toAddress);
                throw new RuntimeException("Wallet not found: " + toAddress);
            }

            // dont auto pay last order, because it is a redo, the order perhaps expired
            onDeposit.onDeposit(transferEvent.getTransactionHash(),
                    fromAddress, toAddress, amountDecimal,
                    System.getenv("TRON_USDT_CONTRACT"),
                    chainType, chainId,
                    transferEvent.getBlockNumber(), false);

        } catch (IOException e) {
            log.error("Failed to check transaction: {}", hash, e);
        }
    }
}
