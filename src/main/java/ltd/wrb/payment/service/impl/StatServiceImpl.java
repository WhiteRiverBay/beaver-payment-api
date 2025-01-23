package ltd.wrb.payment.service.impl;

import ltd.wrb.payment.service.StatService;
import ltd.wrb.payment.util.RedisUtils;
import ltd.wrb.payment.enums.TradeStatus;
import ltd.wrb.payment.enums.TradeType;
import ltd.wrb.payment.model.stat.ChainIdAmount;
import ltd.wrb.payment.model.stat.PaymentOrderStat;
import ltd.wrb.payment.model.stat.TradeTypeAmount;
import ltd.wrb.payment.repository.PaymentOrderRepository;
import ltd.wrb.payment.repository.TradeLogRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.hutool.json.JSONUtil;

@Service
public class StatServiceImpl implements StatService {

    @Autowired
    private RedisUtils cache;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private TradeLogRepository tradeLogRepository;

    @Override
    public List<TradeTypeAmount> getAccountLogStatGroupByTradeType(Date start, Date end) {
        String redisKey = "account_log_stat_" + start.getTime() + "_" + end.getTime();

        // get from cache
        String json = (String) cache.get(redisKey);

        if (json != null) {
            return JSONUtil.toList(json, TradeTypeAmount.class);
        }

        List<Object[]> list = tradeLogRepository.statByTradeType(start, end);

        // convert to TradeTypeAmount
        List<TradeTypeAmount> result = new ArrayList<>();
        for (Object[] obj : list) {
            TradeTypeAmount tradeTypeAmount = new TradeTypeAmount();
            Number type = (Number) obj[0];
            tradeTypeAmount.setType(
                    TradeType.values()[type.intValue()]);
            tradeTypeAmount.setAmount((Number) obj[1]);
            result.add(tradeTypeAmount);
        }

        // save to cache
        cache.set(redisKey, JSONUtil.toJsonStr(result), 60 * 5);

        return result;
    }

    @Override
    public List<ChainIdAmount> getChainDepositStatGroupByChainId(Date start, Date end) {
        String redisKey = "chain_deposit_stat_" + start.getTime() + "_" + end.getTime();

        // get from cache
        String json = (String) cache.get(redisKey);

        if (json != null) {
            return JSONUtil.toList(json, ChainIdAmount.class);
        }

        List<Object[]> list = tradeLogRepository.statByChainId(start, end, TradeType.DEPOSIT);

        // convert to ChainIdAmount
        List<ChainIdAmount> result = new ArrayList<>();
        for (Object[] obj : list) {
            ChainIdAmount chainIdAmount = new ChainIdAmount();
            chainIdAmount.setChainId((Number) obj[0]);
            chainIdAmount.setAmount((Number) obj[1]);
            result.add(chainIdAmount);
        }

        // save to cache
        cache.set(redisKey, JSONUtil.toJsonStr(result), 60 * 5);

        return result;
    }

    @Override
    public List<PaymentOrderStat> getOrderStatGroupByStatus(Date start, Date end) {

        String redisKey = "order_stat_" + start.getTime() + "_" + end.getTime();

        // get from cache
        String json = (String) cache.get(redisKey);

        if (json != null) {
            return JSONUtil.toList(json, PaymentOrderStat.class);
        }

        List<Object[]> list = paymentOrderRepository.countByStatusGroupByStatus(start, end);

        // convert to PaymentOrderStat
        List<PaymentOrderStat> result = new ArrayList<>();
        for (Object[] obj : list) {
            PaymentOrderStat paymentOrderStat = new PaymentOrderStat();
            Number status = (Number) obj[0];
            paymentOrderStat.setStatus(
                    TradeStatus.values()[status.intValue()]);
            paymentOrderStat.setCount((Number) obj[1]);
            paymentOrderStat.setSumAmount((Number) obj[2]);
            paymentOrderStat.setAvgAmount((Number) obj[3]);
            paymentOrderStat.setMaxAmount((Number) obj[4]);
            result.add(paymentOrderStat);
        }

        // save to cache
        cache.set(redisKey, JSONUtil.toJsonStr(result), 60 * 5);

        return result;
    }
}
