package ltd.wrb.payment.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Async;

import ltd.wrb.payment.contracts.IERC20Metadata;

public class Web3jUtil {

    public static Web3j getWeb3(String url, int pollingInterval) {
        HttpService hs = new HttpService(url);
        Web3j web3j = Web3j.build(hs, pollingInterval, Async.defaultExecutorService());
        return web3j;
    }

    public static Web3j getWeb3(String url) throws ConnectException {
        if (url.startsWith("http")) {
            HttpService hs = new HttpService(url);
            Web3j web3j = Web3j.build(hs);
            return web3j;
        } else if (url.startsWith("wss://") || url.startsWith("ws://")) {
            WebSocketService ws = new WebSocketService(url, true);
            Web3j web3j = Web3j.build(ws);
            // ws.connect();
            ws.connect();
            return web3j;
        }
        throw new IllegalArgumentException("invalid url:" + url);
    }

    // getErc20Balance
    public static BigInteger getErc20Balance(Web3j web3, String address, String contractAddress, int chainId)
            throws Exception {
        IERC20Metadata erc20 = IERC20Metadata.load(contractAddress, web3,
                getTransactionManager(web3, getRandomCredentials(), chainId),
                getGasProvider(web3));
        BigInteger balance = erc20.balanceOf(address).send();
        return balance;
    }

    // get erc20 allowance
    public static BigInteger getErc20Allowance(Web3j web3, String contractAddress, String source, String to,
            int chainId)
            throws Exception {
        IERC20Metadata erc20 = IERC20Metadata.load(contractAddress, web3,
                getTransactionManager(web3, getRandomCredentials(), chainId),
                getGasProvider(web3));
        BigInteger balance = erc20.allowance(source, to).send();
        return balance;
    }

    public static Credentials getRandomCredentials()
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        ECKeyPair keyPair = Keys.createEcKeyPair();
        Credentials credentials = Credentials.create(keyPair);
        return credentials;
    }

    public static TransactionManager getTransactionManager(Web3j web3j, Credentials credentials, int chainId) {
        TransactionManager transactionManager = new RawTransactionManager(web3j, credentials, chainId);
        return transactionManager;
    }

    public static IERC20Metadata getErc20Contract(String address, Web3j web3, int chainId) throws Exception {

        Credentials credentials = getRandomCredentials();
        TransactionManager transactionManager = getTransactionManager(web3, credentials, chainId);
        IERC20Metadata res = IERC20Metadata.load(address, web3, transactionManager,
                getGasProvider(web3, getGasPrice(web3), BigInteger.valueOf(1000000)));
        return res;
    }

    public static TransactionReceipt getTransactionReceipt(Web3j web3, String txHash) throws Exception {
        EthGetTransactionReceipt r = web3.ethGetTransactionReceipt(txHash).send();
        if (r != null && r.getTransactionReceipt().isPresent()) {
            return r.getTransactionReceipt().get();
        }
        return null;
    }

    public static Transaction geTransaction(Web3j web3, String txHash) throws Exception {
        return web3.ethGetTransactionByHash(txHash).send().getTransaction().get();
    }

    public static BigInteger getGasPrice(Web3j web3) throws IOException {
        return web3.ethGasPrice().send().getGasPrice();
    }

    public static ContractGasProvider getGasProvider(Web3j web3, BigInteger gasPrice, BigInteger gasLimit) {
        return new StaticGasProvider(gasPrice, gasLimit);
    }

    public static ContractGasProvider getGasProvider(Web3j web3) throws IOException {
        return getGasProvider(web3, gasPrice(web3), gasLimit());
    }

    public static BigInteger gasPrice(Web3j web3) throws IOException {
        return web3.ethGasPrice().send().getGasPrice();
    }

    public static BigInteger gasLimit() {
        return new BigInteger("21000");
    }

    // to decimal
    public static BigDecimal toDecimal(BigInteger value, int decimals) {
        return new BigDecimal(value).divide(
                new BigDecimal(BigInteger.TEN.pow(decimals)),
                8,
                RoundingMode.CEILING);
    }

    // to biginteger
    public static BigInteger toBigInteger(BigDecimal value, int decimals) {
        return value.multiply(new BigDecimal(BigInteger.TEN.pow(decimals))).toBigInteger();
    }

    // is valid address
    public static boolean isAddressValid(String address) {
        // return address != null && address.startsWith("0x") && address.length() == 42;
        // return "^0x[a-zA-Z]{40}$".matc
        return address.matches("^0x[a-zA-Z0-9]{40}$");
    }

    // short address
    public static String shortAddress(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }
}