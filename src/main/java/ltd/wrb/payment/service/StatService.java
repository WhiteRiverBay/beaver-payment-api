package ltd.wrb.payment.service;

import java.util.Date;
import java.util.List;

import ltd.wrb.payment.model.stat.ChainIdAmount;
import ltd.wrb.payment.model.stat.PaymentOrderStat;
import ltd.wrb.payment.model.stat.TradeTypeAmount;

/**
 * StatService
 * 
 * @version 1.0
 */
public interface StatService {
    /**
     * get account log stat group by trade type
     * 历史累计
     * 今日新增
     * （类型，总金额）
     * @param start
     * @param end
     * @return
     */
    List<TradeTypeAmount> getAccountLogStatGroupByTradeType(
        Date start,
        Date end
    );

    /**
     * get chain deposit stat group by chain id
     * 各条链累计充值金额
     * 今日新增充值金额
     * @param start
     * @param end
     * @return
     */
    List<ChainIdAmount> getChainDepositStatGroupByChainId(
        Date start,
        Date end
    );

    /**
     * get order stat group by status
     * 历史累计各类订单数
     * 今日新增各类订单数
     * @param start
     * @param end
     * @return
     */
    List<PaymentOrderStat> getOrderStatGroupByStatus(
        Date start,
        Date end
    );
}
