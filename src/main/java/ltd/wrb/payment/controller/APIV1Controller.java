package ltd.wrb.payment.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.controller.dto.ChainDescribe;
import ltd.wrb.payment.controller.dto.ExceptionHashDTO;
import ltd.wrb.payment.controller.dto.PaymentDTO;
import ltd.wrb.payment.controller.dto.RefundDTO;
import ltd.wrb.payment.controller.dto.WithdrawApplication;
import ltd.wrb.payment.controller.dto.WithdrawConfirmDTO;
import ltd.wrb.payment.controller.dto.WithdrawReject;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.enums.TradeStatus;
import ltd.wrb.payment.job.Redo;
import ltd.wrb.payment.model.EVMChainConfig;
import ltd.wrb.payment.model.EVMChainConfigToken;
import ltd.wrb.payment.model.PaymentOrder;
import ltd.wrb.payment.model.TradeLog;
import ltd.wrb.payment.model.Wallet;
import ltd.wrb.payment.service.AccountService;
import ltd.wrb.payment.service.AccountServiceException;
import ltd.wrb.payment.service.PaymentService;
import ltd.wrb.payment.service.WalletService;
import ltd.wrb.payment.util.IpLimiter;
import ltd.wrb.payment.util.RedisUtils;
import ltd.wrb.payment.util.Result;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class APIV1Controller {

    @Autowired
    private WalletService walletService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private RedisUtils cache;

    @Autowired
    private Redo redo;

    @GetMapping("/address/{id}")
    public Result getAddress(
            @PathVariable("id") String id) {
        PaymentOrder order = paymentService.getById(id);
        if (order == null) {
            return Result.error("Not found");
        }
        return Result.success(walletService.getWallets(order.getUid()));
    }

    @IpLimiter(limit = 600, time = 60)
    @PostMapping("/order")
    public Result createOrder(@RequestBody PaymentDTO paymentDTO) {
        try {
            // dump paymentDTO
            log.info("[PaymentDTO] {}", JSONUtil.toJsonStr(paymentDTO));
            // check timestamp
            if (paymentDTO.getTimestamp() + 5 * 60 * 1000 < System.currentTimeMillis()) {
                return Result.error("Expired timestamp");
            }
            // check sign
            if (!paymentDTO.isValidSign()) {
                return Result.error("Invalid sign");
            }
            // check nonce
            String key = "nonce:" + paymentDTO.getNonce();
            if (!cache.lock(key, 85400)) {
                return Result.error("Invalid nonce");
            }
            // find the order by tradeNo if exists and not expired and not paid, return it
            // directly
            PaymentOrder order = paymentService.getByOid(paymentDTO.getOid());
            if (order != null && order.getStatus() == TradeStatus.PENDING_PAY) {
                return Result.success(order);
            }

            order = PaymentOrder.builder()
                    .id(IdUtil.getSnowflake(1, 1).nextIdStr())
                    .uid(paymentDTO.getUid())
                    .oid(paymentDTO.getOid())
                    .amount(paymentDTO.getAmount())
                    .memo(paymentDTO.getMemo())
                    .expiredAt(paymentDTO.getExpiredAt())
                    .mchId(paymentDTO.getMchId())
                    .status(TradeStatus.PENDING_PAY)
                    .logo(paymentDTO.getLogo())
                    .redirectUrl(paymentDTO.getRedirectUrl())
                    .notifyUrl(paymentDTO.getNotifyUrl() == null ? System.getenv("DEFAULT_CALLBACK_URL")
                            : paymentDTO.getNotifyUrl())
                    .build();
            paymentService.save(order);

            Map<ChainType, String> res = walletService.initWallets(paymentDTO.getUid());
            order.setAddresses(res);
            return Result.success(order);
        } catch (Exception e) {
            log.error("Create order error", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/order/{id}/status")
    public Result orderPayStatus(@PathVariable("id") String id) {
        PaymentOrder order = paymentService.getById(id);
        if (order == null) {
            return Result.error("Order not found");
        }
        Date now = new Date();
        if (order.getExpiredAt() < now.getTime()
                && order.getStatus() == TradeStatus.PENDING_PAY) {
            return Result.success(TradeStatus.EXPIRED);
        }
        return Result.success(order.getStatus());
    }

    @GetMapping("/order/{id}")
    public Result getOrder(@PathVariable("id") String id) {
        PaymentOrder order = paymentService.getById(id);
        return Result.success(order);
    }

    @GetMapping("/order/{id}/lastDeposit")
    public Result getLastDeposit(@PathVariable("id") String id) {
        PaymentOrder order = paymentService.getById(id);
        if (order == null) {
            return Result.error("Order not found");
        }

        String uid = order.getUid();
        Map<ChainType, String> wallets = walletService.getWallets(uid);
        if (wallets == null || wallets.isEmpty()) {
            return Result.error("Wallet not found");
        }

        for (Map.Entry<ChainType, String> entry : wallets.entrySet()) {
            String keyDepositForOrder = "deposit_for_order:" + entry.getValue();
            String depositForOrder = (String) (cache.get(keyDepositForOrder));
            if (depositForOrder != null) {
                return Result.success(JSONUtil.parse(depositForOrder));
            }
        }

        return Result.success(null);
    }

    @GetMapping("/balance/{uid}")
    public Result getBalance(
            @PathVariable("uid") String uid) {
        return Result.success(accountService.getBalance(uid));
    }

    @IpLimiter(limit = 5, time = 60)
    @PostMapping("/pay/{id}")
    public Result payDriectly(
            @PathVariable("id") String id) {
        PaymentOrder order = paymentService.getById(id);
        if (order == null) {
            return Result.error("Order not found");
        }
        if (order.getStatus() != TradeStatus.PENDING_PAY) {
            return Result.error("Order status is not PENDING_PAY");
        }

        try {
            paymentService.tryPayLastOrder(order.getUid(), true);
            return Result.SUCCESS;
        } catch (AccountServiceException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/exception/hash")
    @IpLimiter(limit = 10, time = 60)
    public Result exceptionHash(@RequestBody ExceptionHashDTO tx) {
        log.info("[ExceptionHashDTO] {}", JSONUtil.toJsonStr(tx));

        if (!tx.isValid()) {
            return Result.error("Invalid arguments");
        }

        try {
            if (tx.getChainType() == ChainType.TRON) {
                redo.checkTron(tx.getHash());
                return Result.SUCCESS;
            } else if (tx.getChainType() == ChainType.EVM) {
                redo.checkEth(tx.getHash(), tx.getChainId());
                return Result.SUCCESS;
            } else {
                return Result.error("Unsupported chain type");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // supported chain types
    @GetMapping("/chains")
    public Result getChainTypes() {
        List<ChainDescribe> res = new ArrayList<>();

        Map<Integer, EVMChainConfig> config = EVMChainConfig.load();
        for (Map.Entry<Integer, EVMChainConfig> entry : config.entrySet()) {
            res.add(ChainDescribe.builder().chainName(entry.getValue().getChainName())
                    .chainType(ChainType.EVM)
                    .chainId(entry.getKey())
                    .description(entry.getValue().getDepositDescription())
                    .descriptionMap(
                            entry.getValue().getDescription() == null ? null : entry.getValue().getDescription())
                    .usdtContracts(entry.getValue().getUsdtContracts())
                    .confirmBlocksRequired(entry.getValue().getConfirmBlock())
                    .confirmTimeDelaySeconds(entry.getValue().getConfirmTimeDelaySeconds())
                    .confirmThresholdAmountInDecimals(entry.getValue().getConfirmThresholdAmountInDecimals())
                    .build());
        }

        if (Boolean.parseBoolean(System.getenv("TRON_USDT_ENABLED"))) {

            EVMChainConfigToken token = new EVMChainConfigToken();
            token.setAddress(System.getenv("TRON_USDT_CONTRACT"));
            token.setDecimals(6);
            token.setSymbol("USDT");

            res.add(ChainDescribe.builder().chainName("TRON")
                    .chainType(ChainType.TRON)
                    .chainId(728126428)
                    .descriptionMap(
                            Map.of("TRC20", "TRC20 USDT"))
                    .usdtContracts(Arrays.asList(token))
                    .confirmBlocksRequired(0)
                    .confirmTimeDelaySeconds(10)
                    .confirmThresholdAmountInDecimals(BigDecimal.valueOf(0))
                    .description("TRC20").build());
        }

        return Result.success(res);
    }

    // get address by chain type
    @GetMapping("/address/{id}/{chainType}")
    public Result getAddressByChainType(
            @PathVariable("id") String id,
            @PathVariable("chainType") ChainType chainType) {

        PaymentOrder order = paymentService.getById(id);
        if (order == null) {
            return Result.error("Order not found");
        }
        Wallet wallet = walletService.getByAddressAndChainType(order.getUid(), chainType);
        return Result.success(wallet.getAddress());
    }

    // refund a payment order
    @PostMapping("/refund")
    public Result refundOrder(@RequestBody RefundDTO refundDTO) {

        log.info("[RefundDTO] {}", JSONUtil.toJsonStr(refundDTO));
        if (!refundDTO.isValidSign()) {
            return Result.error("Invalid sign");
        }

        PaymentOrder order = refundDTO.getId() == null ? paymentService.getByOid(refundDTO.getOid())
                : paymentService.getById(refundDTO.getId());
        if (order == null) {
            return Result.error("Order not found");
        }
        if (order.getStatus() != TradeStatus.PAID) {
            return Result.error("Order status is not PAID");
        }

        if (order.getAmount().compareTo(refundDTO.getAmount()) < 0) {
            return Result.error("Refund amount is greater than order amount");
        }

        // amount can not be negative
        if (refundDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("Invalid amount");
        }
        try {
            paymentService.refund(order, refundDTO.getAmount(), refundDTO.getRemark());
            return Result.SUCCESS;
        } catch (AccountServiceException e) {
            return Result.error(e.getMessage());
        }
    }

    // withdraw application
    @PostMapping("/withdraw/apply")
    public Result withdraw(@RequestBody WithdrawApplication applicationDTO) {

        if (!applicationDTO.isValidSign()) {
            return Result.error("Invalid sign");
        }

        log.info("[WithdrawDTO] {}", JSONUtil.toJsonStr(applicationDTO));

        if (applicationDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("Invalid amount");
        }

        try {
            TradeLog tradeLog = accountService.withdrawApply(applicationDTO.getUid(), applicationDTO.getAmount(),
                    applicationDTO.getTo(), applicationDTO.getChainType(), applicationDTO.getChainId(),
                    applicationDTO.getToken());
            return Result.success(tradeLog);
        } catch (AccountServiceException e) {
            return Result.error(e.getMessage());
        }
    }

    // confirm withdraw
    @PostMapping("/withdraw/confirm")
    public Result withdrawConfirm(@RequestBody WithdrawConfirmDTO dto) {
        if (!dto.isValidSign()) {
            return Result.error("Invalid sign");
        }
        log.info("[WithdrawConfirmDTO] {}", JSONUtil.toJsonStr(dto));
        try {
            accountService.withdrawConfirm(dto.getLogId(), dto.getTxHash(), dto.getBlockNumber());
            return Result.SUCCESS;
        } catch (AccountServiceException e) {
            return Result.error(e.getMessage());
        }
    }

    // reject withdraw
    @PostMapping("/withdraw/reject")
    public Result rejectWithdraw(@RequestBody WithdrawReject dto) {
        if (!dto.isValidSign()) {
            return Result.error("Invalid sign");
        }
        try {
            accountService.rejectWithdraw(dto.getId());
            return Result.SUCCESS;
        } catch (AccountServiceException e) {
            return Result.error(e.getMessage());
        }
    }
}
