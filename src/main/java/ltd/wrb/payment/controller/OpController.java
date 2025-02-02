package ltd.wrb.payment.controller;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.Predicate;
import ltd.wrb.payment.controller.dto.Ga;
import ltd.wrb.payment.controller.dto.WalletDumpDTO;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.enums.TradeType;
import ltd.wrb.payment.job.DepositJob;
import ltd.wrb.payment.model.TradeLog;
import ltd.wrb.payment.model.Wallet;
import ltd.wrb.payment.repository.TradeLogRepository;
import ltd.wrb.payment.repository.WalletRepository;
import ltd.wrb.payment.util.GAUtil;
import ltd.wrb.payment.util.IpLimiter;
import ltd.wrb.payment.util.RedisUtils;
import ltd.wrb.payment.util.Result;

@RestController
@RequestMapping("/_op")
public class OpController {

    @Autowired
    private RedisUtils cache;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private DepositJob depositJob;

    @Autowired
    private TradeLogRepository tradeLogRepository;

    @RequestMapping("/checkBlock")
    public Result checkBlock(BigInteger begin, BigInteger end) {
        return Result.SUCCESS;
    }

    @RequestMapping("/dumpWalletHasDepositLog")
    public Result dumpWalletHasDepositLog(
            @RequestHeader("Authorization") String token,
            @RequestBody WalletDumpDTO walletDumpDTO) {
        if (!getClientSecret().equals(token)) {
            return Result.error("invalid token");
        }
        // 从deposit log中查询所有的wallet distinct，然后join到wallet表里的密码数据，然后dump
        return Result.SUCCESS;
    }

    @RequestMapping("/dumpWallet/{chainType}")
    @IpLimiter
    public Result dumpWallet(@RequestBody Ga ga,
            @RequestHeader("Authorization") String token,
            @PathVariable("chainType") ChainType chainType) {
        if (!getClientSecret().equals(token)) {
            return Result.error("invalid token");
        }
        String keyLocker = "wallet:locker:op:global";
        if (!cache.lock(keyLocker, 2)) {
            return Result.error("too many requests");
        }

        // check code
        String secret = System.getenv("GA_SECRET");

        if (secret == null || secret.isEmpty()) {
            return Result.error("secret not found");
        }

        GAUtil gaUtil = new GAUtil();
        gaUtil.setWindowSize(2);
        if (!gaUtil.check_code(secret, ga.getCode(), System.currentTimeMillis())) {
            return Result.error("invalid code");
        }

        List<Wallet> wallets = walletRepository.findByChainType(chainType);
        return Result.success(wallets);
    }

    @RequestMapping("/restartScanner/{chainId}")
    public Result restartScanner(@PathVariable("chainId") Integer chainId,
            @RequestHeader("Authorization") String token) {
        if (!getClientSecret().equals(token)) {
            return Result.error("invalid token");
        }
        depositJob.restartScanner(chainId);
        return Result.SUCCESS;
    }

    @RequestMapping("/isScannerRunning")
    public Result isScannerRunning(
            @RequestHeader("Authorization") String token) {
        if (!getClientSecret().equals(token)) {
            return Result.error("invalid token");
        }
        return Result.success(depositJob.getScannerStatus());
    }

    // cpu and memory
    @RequestMapping("/getRuntime")
    public Result getCpuAndMemory(
            @RequestHeader("Authorization") String token) {
        if (!getClientSecret().equals(token)) {
            return Result.error("invalid token");
        }

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        int availableProcessors = osBean.getAvailableProcessors();
        double systemLoadAverage = osBean.getSystemLoadAverage();
        // thread
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long totalThreadCount = threadMXBean.getThreadCount();
        long peakThreadCount = threadMXBean.getPeakThreadCount();
        long currentThreadCount = threadMXBean.getThreadCount();

        long totalDiskSpace = new File("/").getTotalSpace();
        long freeDiskSpace = new File("/").getFreeSpace();

        Map<String, Object> result = new HashMap<>();
        result.put("cpu_cores", availableProcessors);
        result.put("system_load_average", systemLoadAverage);
        result.put("jvm_total_memory_mb", totalMemory / 1024 / 1024);
        result.put("jvm_free_memory_mb", freeMemory / 1024 / 1024);
        result.put("jvm_max_memory_mb", maxMemory / 1024 / 1024);
        result.put("jvm_used_memory_mb", usedMemory / 1024 / 1024);
        result.put("total_disk_space_gb", totalDiskSpace / 1024 / 1024 / 1024);
        result.put("total_disk_space_gb", totalDiskSpace / 1024 / 1024 / 1024);
        result.put("free_disk_space_gb", freeDiskSpace / 1024 / 1024 / 1024);
        result.put("total_thread_count", totalThreadCount);
        result.put("peak_thread_count", peakThreadCount);
        result.put("current_thread_count", currentThreadCount);

        return Result.success(result);
    }

    @RequestMapping("/getTradeLogs")
    public Result getTradeLogs(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "uid", required = false) String uid,
            @RequestParam(value = "paymentId", required = false) String paymentId,
            @RequestParam(value = "type", required = false) TradeType type,
            @RequestParam(value = "txHash", required = false) String txHash,
            @RequestParam(value = "token", required = false) String contractTokenAddress,
            @RequestParam(value = "chainId", required = false) Integer chainId,
            @RequestParam(value = "txFrom", required = false) String txFrom,
            @RequestParam(value = "txTo", required = false) String txTo,
            @RequestParam(value = "begin", required = false) Long begin,
            @RequestParam(value = "end", required = false) Long end,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Direction.DESC) Pageable pageable) {
        if (!getClientSecret().equals(token)) {
            return Result.error("invalid token");
        }

        Page<TradeLog> tradeLogs = tradeLogRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (id != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), id));
            }
            if (uid != null) {
                predicates.add(criteriaBuilder.equal(root.get("uid"), uid));
            }
            if (paymentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentId"), paymentId));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (txHash != null) {
                predicates.add(criteriaBuilder.equal(root.get("txHash"), txHash));
            }
            if (contractTokenAddress != null) {
                predicates.add(criteriaBuilder.equal(root.get("token"), contractTokenAddress));
            }
            if (chainId != null) {
                predicates.add(criteriaBuilder.equal(root.get("chainId"), chainId));
            }
            if (txFrom != null) {
                predicates.add(criteriaBuilder.equal(root.get("txFrom"), txFrom));
            }
            if (txTo != null) {
                predicates.add(criteriaBuilder.equal(root.get("txTo"), txTo));
            }
            if (begin != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), begin));
            }
            if (end != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), end));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        }, pageable);

        return Result.success(tradeLogs);
    }

    private String getClientSecret() {
        return System.getenv("CLIENT_SECRET");
    }
}
