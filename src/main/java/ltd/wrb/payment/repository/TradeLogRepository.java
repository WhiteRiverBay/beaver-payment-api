package ltd.wrb.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

import ltd.wrb.payment.enums.TradeType;
import ltd.wrb.payment.model.TradeLog;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long>, JpaSpecificationExecutor<TradeLog> {

    @Query("select sum(amount) from TradeLog where uid = :uid and status = 1 AND confirmedBlocks >= confirmedBlocksRequired")
    Number sumAmountByUid(@Param("uid") String uid);

    // sum amount by uid and tradeType
    @Query("select sum(amount) from TradeLog where uid = :uid and type = :tradeType and status = 1 AND confirmedBlocks >= confirmedBlocksRequired")
    Number sumAmountByUidAndTradeType(
            @Param("uid") String uid,
            @Param("tradeType") TradeType tradeType);

    // exists by txHash
    boolean existsByTxHash(String txHash);

    // stat by trade type, filter by create time
    @Query("select type, sum(amount) from TradeLog where createdAt >= :start and createdAt < :end group by type")
    List<Object[]> statByTradeType(
            @Param("start") Date start,
            @Param("end") Date end);

    // stat by chain id, filter by create time and trade type, group by chain id
    @Query("select chainId, sum(amount) from TradeLog where createdAt >= :start and createdAt < :end and type = :tradeType group by chainId")
    List<Object[]> statByChainId(
            @Param("start") Date start,
            @Param("end") Date end,
            @Param("tradeType") TradeType tradeType);

    @Query("SELECT type, SUM(amount), COUNT(*), AVG(amount), MAX(amount), MIN(amount) " +
            "FROM TradeLog WHERE createdAt >= :begin AND createdAt < :end GROUP BY type")
    List<Object[]> sumStatByType(@Param("begin") long begin, @Param("end") long end);

    // created at formatted to days and sum amount
    @Query("SELECT FUNCTION('DATE_FORMAT', createdAt, '%m-%d') AS day, SUM(amount) " +
            "FROM TradeLog WHERE createdAt >= :begin AND createdAt < :end AND type=:type GROUP BY FUNCTION('DATE_FORMAT', createdAt, '%m-%d')")
    List<Object[]> trendStatByType(@Param("begin") long begin, @Param("end") long end, @Param("type") TradeType type);

    @Query("SELECT chainId, SUM(amount), COUNT(*), COUNT(DISTINCT txTo) " +
            "FROM TradeLog WHERE createdAt >= :begin AND createdAt < :end GROUP BY chainId")
    List<Object[]> depositStatByChain(@Param("begin") long begin, @Param("end") long end);

    @Query("FROM TradeLog WHERE confirmedBlocksRequired > 0 AND confirmedBlocks < confirmedBlocksRequired AND type = :type AND status = 1")
    List<TradeLog> findByNeedConfirmLogs(@Param("type") TradeType type);
}
