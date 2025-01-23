package ltd.wrb.payment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.json.JSONUtil;
import ltd.wrb.payment.enums.TradeType;
import ltd.wrb.payment.repository.TradeLogRepository;
import ltd.wrb.payment.util.RedisUtils;
import ltd.wrb.payment.util.Result;

import java.util.List;

@RequestMapping("/_stat")
@RestController
public class StatController {

    @Autowired
    private TradeLogRepository tradeLogRepository;

    @Autowired
    private RedisUtils cache;

    /*
     * [
     * [type, sum(amount), count(*), avg(amount), max(amount), min(amount)],
     * ]
     */
    @RequestMapping("/sum")
    public Result sumStatByType(
            @RequestHeader("Authorization") String token,
            long begin,
            long end) {

        if (!getClientSecret().equals(token)) {
            return Result.error("invalid token");
        }

        String key = String.format("sum:%d:%d", begin, end);
        if (cache.hasKey(key)) {
            Object json = cache.get(key);
            return Result.success(
                    JSONUtil.parseArray(json.toString()));
        }

        List<Object[]> stats = tradeLogRepository.sumStatByType(begin, end);
        return Result.success(stats);
    }

    // trend [create_time, sum(amount)]
    @RequestMapping("/trend")
    public Result trendStatByType(
            @RequestHeader("Authorization") String token,
            long begin,
            long end,
            TradeType type) {
        if (!getClientSecret().equals(token)) {
            return Result.error("invalid token");
        }
        String key = String.format("trend:%s:%d:%d", type, begin, end);
        if (cache.hasKey(key)) {
            Object json = cache.get(key);
            return Result.success(
                    JSONUtil.parseArray(json.toString()));
        }

        List<Object[]> trend = tradeLogRepository.trendStatByType(begin, end, type);
        cache.set(key, JSONUtil.toJsonStr(trend), 300);
        return Result.success(trend);
    }

    // deposit by chain
    // [chain_name, chain_id, sum(amount), count(*), count(distinct to_address)]
    @RequestMapping("/deposit")
    public Result depositStatByChain(
            @RequestHeader("Authorization") String token,
            long begin,
            long end) {
        if (!getClientSecret().equals(token)) {
            return Result.error("invalid token");
        }
        String key = String.format("deposit:%d:%d", begin, end);
        if (cache.hasKey(key)) {
            Object json = cache.get(key);
            return Result.success(
                    JSONUtil.parseArray(json.toString()));
        }

        List<Object[]> depositStats = tradeLogRepository.depositStatByChain(begin, end);
        cache.set(key, JSONUtil.toJsonStr(depositStats), 300);
        return Result.success(depositStats);
    }

    private String getClientSecret() {
        return System.getenv("CLIENT_SECRET");
    }
}
