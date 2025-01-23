package ltd.wrb.payment.service.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.enums.NotifyStatus;
import ltd.wrb.payment.enums.TradeStatus;
import ltd.wrb.payment.enums.TradeType;
import ltd.wrb.payment.model.NotifyTask;
import ltd.wrb.payment.model.PaymentOrder;
import ltd.wrb.payment.model.TradeLog;
import ltd.wrb.payment.repository.PaymentOrderRepository;
import ltd.wrb.payment.service.AccountService;
import ltd.wrb.payment.service.AccountServiceException;
import ltd.wrb.payment.service.NotifyService;
import ltd.wrb.payment.service.PaymentService;
import ltd.wrb.payment.util.RedisUtils;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private RedisUtils cache;

    @Override
    public void save(PaymentOrder order) {
        // TODO: oid is unique
        paymentOrderRepository.save(order);
    }

    @Override
    public PaymentOrder getById(String id) {
        return paymentOrderRepository.findById(id).orElse(null);
    }

    @Override
    public PaymentOrder getByOid(String oid) {
        return paymentOrderRepository.findByOid(oid);
    }

    @Override
    @Transactional
    public void tryPayLastOrder(String uid, boolean closeOthers) throws AccountServiceException {
        PageRequest page = PageRequest.of(0, 1, Sort.by(Direction.DESC, "createdAt"));
        Page<PaymentOrder> orders = paymentOrderRepository.findByUidAndStatus(uid, TradeStatus.PENDING_PAY, page);
        if (orders.isEmpty()) {
            return;
        }

        PaymentOrder order = orders.getContent().get(0);

        BigDecimal balance = accountService.getBalance(uid);
        if (balance.compareTo(order.getAmount()) >= 0) {
            // TradeLog
            TradeLog tradeLog = TradeLog.builder().uid(uid)
                    .amount(order.getAmount().multiply(new BigDecimal(-1)))
                    .memo("Pay order: " + order.getId())
                    .type(TradeType.PAYMENT)
                    .paymentId(order.getId())
                    .createdAt(System.currentTimeMillis())
                    .build();
            accountService.addTradeLog(tradeLog);
            // pay
            order.setStatus(TradeStatus.PAID);
            paymentOrderRepository.save(order);
            if (closeOthers) {
                // close other orders
                paymentOrderRepository.findByUidAndStatus(uid, TradeStatus.PENDING_PAY).forEach(o -> {
                    if (!o.getId().equals(order.getId())) {
                        o.setStatus(TradeStatus.CLOSED);
                        paymentOrderRepository.save(o);
                    }
                });
            }
            // add callback queue
            NotifyTask task = NotifyTask.builder()
                    .createdAt(System.currentTimeMillis())
                    .nextNotifyTime(System.currentTimeMillis())
                    .paymentId(order.getId())
                    .status(NotifyStatus.PENDING)
                    .retryCount(0)
                    .updatedAt(System.currentTimeMillis())
                    .uid(uid)
                    .oid(order.getOid())
                    .url(order.getNotifyUrl())
                    .build();
            notifyService.addTask(task, true);
        } else {
            log.warn("Not enough balance to pay order: {}", order);
        }

    }

    @Override
    @Transactional
    public void refund(PaymentOrder order, BigDecimal amount, String remark) throws AccountServiceException {
        if (order == null) {
            throw new AccountServiceException("Order not found");
        }
        if (order.getStatus() != TradeStatus.PAID) {
            throw new AccountServiceException("Order not paid");
        }
        // amount > 0
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountServiceException("Invalid amount");
        }
        // amount <= order.getAmount()
        if (amount.compareTo(order.getAmount()) > 0) {
            throw new AccountServiceException("Refund amount exceeds order amount");
        }
        String lockKey = "refund_" + order.getId();
        if (!cache.lock(lockKey, 60)) {
            throw new AccountServiceException("Refund already in process");
        }
        // TradeLog
        TradeLog tradeLog = TradeLog.builder().uid(order.getUid())
                .amount(amount)
                .memo("Refund order: " + order.getId())
                .type(TradeType.REFUND)
                .paymentId(order.getId())
                .createdAt(System.currentTimeMillis())
                .build();
        accountService.addTradeLog(tradeLog);

        // refund
        order.setStatus(TradeStatus.REFUNDED);
        paymentOrderRepository.save(order);
    }

}
