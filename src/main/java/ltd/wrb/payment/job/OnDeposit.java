package ltd.wrb.payment.job;

import java.math.BigDecimal;
import java.math.BigInteger;

import ltd.wrb.payment.enums.ChainType;

public interface OnDeposit {

    boolean isWalletExist(String address, ChainType chainType);

    void onDeposit(String txHash, String from, String to, BigDecimal amount, String token, ChainType chainType, Integer chainId,
            BigInteger blockNumber);

    void onDeposit(String txHash, String from, String to, BigDecimal amount, String token, ChainType chainType, Integer chainId,
            BigInteger blockNumber, boolean autoPayLastOrder);
}
