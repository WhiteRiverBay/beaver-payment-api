package ltd.wrb.payment.service;

import java.math.BigDecimal;
import java.math.BigInteger;

import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.model.TradeLog;

public interface AccountService {

    BigDecimal getBalance(String uid);

    void addTradeLog(TradeLog tradeLog) throws AccountServiceException;

    boolean isTradeLogExist(String txHash);

    TradeLog withdrawApply(String uid, BigDecimal amount, String to, ChainType chainType, Integer chainId, String token) throws AccountServiceException;

    void withdrawConfirm(Long logId, String txHash, BigInteger blockNumber) throws AccountServiceException;

    void rejectWithdraw(Long logId) throws AccountServiceException;
    
}
