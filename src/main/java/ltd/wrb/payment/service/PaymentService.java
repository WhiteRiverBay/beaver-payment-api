package ltd.wrb.payment.service;

import ltd.wrb.payment.model.PaymentOrder;

public interface PaymentService {

    void save(PaymentOrder order);

    PaymentOrder getById(String id);

    // get by oid
    PaymentOrder getByOid(String oid);

    void tryPayLastOrder(String uid, boolean closeOthers) throws AccountServiceException;

    // refund
    void refund(PaymentOrder order, java.math.BigDecimal amount, String remark) throws AccountServiceException;
}
