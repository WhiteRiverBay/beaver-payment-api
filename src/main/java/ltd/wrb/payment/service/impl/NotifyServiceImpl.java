package ltd.wrb.payment.service.impl;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.enums.NotifyStatus;
import ltd.wrb.payment.enums.TradeStatus;
import ltd.wrb.payment.model.NotifyTask;
import ltd.wrb.payment.model.PaymentOrder;
import ltd.wrb.payment.repository.NotifyTaskRepository;
import ltd.wrb.payment.repository.PaymentOrderRepository;
import ltd.wrb.payment.service.NotifyService;
import ltd.wrb.payment.util.RedisUtils;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    @Autowired
    private RedisUtils cache;

    @Autowired
    private NotifyTaskRepository notifyTaskRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    private ExecutorService executor = Executors.newFixedThreadPool(20);

    @Override
    public void addTask(NotifyTask task, boolean runNow) {
        notifyTaskRepository.save(task);
        if (runNow) {
            executor.execute(() -> {
                execute(task, true);
            });
        }
    }

    // every 1 min run
    @Scheduled(cron = "0 0/1 * * * ?")
    public void run1mins() {
        long now = System.currentTimeMillis();
        // 创建时间小于等于5分钟，状态为PENDING的任务
        notifyTaskRepository.findByCreatedAtLessThanEqualAndStatus(now - 300000, NotifyStatus.PENDING).forEach(task -> {
            executor.execute(() -> {
                execute(task,false);
            });
        });
    }

    // every 1 hour run
    @Scheduled(cron = "0 0 * * * ?")
    public void run24hours() {
        long now = System.currentTimeMillis();
        // 创建时间大于5分钟，小于等于25小时，状态为PENDING的任务
        notifyTaskRepository.findByCreatedAtGreaterThanAndCreatedAtLessThanEqualAndStatus(now - 90000000, now - 3600000,
                NotifyStatus.PENDING).forEach(task -> {
                    executor.execute(() -> {
                        execute(task, false);
                    });
                });
    }

    /**
     * 
     * @param task
     * @param nextNotifyTimeInMs 60000(1minute) or 3600000(1hour)
     */
    private void execute(NotifyTask task, boolean force) {
        String taskLockKey = "notify_task_lock_" + task.getId();
        if (!cache.lock(taskLockKey, 50)) {
            log.info("Task is locked: {}", task.getId());
            return;
        }
        if (!force && task.getRetryCount() >= 29) {
            task.setStatus(NotifyStatus.FINAL_FAIL);
            task.setUpdatedAt(System.currentTimeMillis());
            task.setExecutedAt(System.currentTimeMillis());
            task.setExecutor("system");
            notifyTaskRepository.save(task);
            return;
        }
        String url = task.getUrl();

        long nextNotifyTimeInMs = task.getRetryCount() > 5 ? 3600000 : 60000;

        PaymentOrder order = paymentOrderRepository.findById(task.getPaymentId()).orElse(null);
        if (order == null) {
            log.error("[task:{}] Order not found, id: {}", task.getId(), task.getPaymentId());
            return;
        }
        // if (!force && order.getStatus() != TradeStatus.PAID) {
            // log.error("[task:{}] Order status is not PAID, id: {}", task.getId(), task.getPaymentId());
            // return;
        // }

        Map<String, Object> params = new TreeMap<>();
        params.put("id", task.getPaymentId());
        params.put("oid", task.getOid());
        params.put("uid", task.getUid());
        params.put("timestamp", System.currentTimeMillis());
        params.put("nonce", IdUtil.fastSimpleUUID());
        params.put("status",TradeStatus.PAID);
        params.put("statusCode", order.getStatus().ordinal());

        String base = MapUtil.join(params, "&", "=");
        String secret = System.getenv("PAYMENT_NOTIFY_SECRET");

        String sign = DigestUtil.sha256Hex(base + secret);
        params.put("sign", sign);

        String jsonBody = JSONUtil.toJsonStr(params);
        log.info("[task:{}] Notify: {} body: {}", task.getId(), url, jsonBody);
        // send request
        try {
            HttpRequest post = HttpRequest.post(url).body(jsonBody).timeout(5000).contentType("application/json");
            String body = post.execute().body();
            if (body.equalsIgnoreCase("success")) {
                task.setStatus(NotifyStatus.SUCCESS);
                task.setExecutedAt(System.currentTimeMillis());
                task.setExecutor("system");
                task.setUpdatedAt(System.currentTimeMillis());
                notifyTaskRepository.save(task);
            } else {
                log.error("[task:{}] Failed to notify, id: {} url: {} resp: {}", task.getId(), task.getPaymentId(), url,
                        body);
                task.setRetryCount(task.getRetryCount() + 1);
                task.setNextNotifyTime(System.currentTimeMillis() + nextNotifyTimeInMs);
                task.setUpdatedAt(System.currentTimeMillis());
                notifyTaskRepository.save(task);
            }
        } catch (Exception e) {

            log.error("Failed to notify: {}", e.getMessage());
            task.setRetryCount(task.getRetryCount() + 1);
            task.setNextNotifyTime(System.currentTimeMillis() + nextNotifyTimeInMs);
            task.setUpdatedAt(System.currentTimeMillis());
            notifyTaskRepository.save(task);
        }
    }

}
