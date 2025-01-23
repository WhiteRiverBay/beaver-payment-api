package ltd.wrb.payment.repository;

import java.util.List;
import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ltd.wrb.payment.enums.TradeStatus;
import ltd.wrb.payment.model.PaymentOrder;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, String> {
    
    // find by uid and status as page
    Page<PaymentOrder> findByUidAndStatus(String uid, TradeStatus status, Pageable pageable);

    // find by uid and status as list
    List<PaymentOrder> findByUidAndStatus(String uid, TradeStatus status);

    // find by oid
    PaymentOrder findByOid(String oid);

    // 各种状态的订单数和金额和平均金额和最大金额
    @Query("select status, count(id), sum(amount), avg(amount), max(amount) from PaymentOrder where createdAt >= :start and createdAt < :end group by status")
    List<Object[]> countByStatusGroupByStatus(
        Date start,
        Date end
    );
}
