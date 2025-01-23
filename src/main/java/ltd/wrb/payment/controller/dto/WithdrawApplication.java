package ltd.wrb.payment.controller.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;

import lombok.Data;
import ltd.wrb.payment.enums.ChainType;

@Data
public class WithdrawApplication {

    private String uid;

    private BigDecimal amount;

    private String to;

    private Integer chainId;

    private ChainType chainType;

    private String token;

    private String notifyUrl;

    private Long timestamp;

    private String nonce;

    private String sign;

    // is valid
    public boolean isValidSign() {
        Map<String, Object> params = Map.of("uid", uid, "amount", amount, "to", to, "chainId", chainId, "chainType",
                chainType, "token", token, "notifyUrl", notifyUrl, "timestamp", timestamp, "nonce", nonce);
        TreeMap<String, Object> sortedParams = new TreeMap<>(params);
        String base = sortedParams.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b).orElse("");
        String signature = DigestUtils.sha256Hex(base + System.getenv("WITHDRAW_NOTIFY_SECRET"));
        return signature.equals(sign);
    }

}
