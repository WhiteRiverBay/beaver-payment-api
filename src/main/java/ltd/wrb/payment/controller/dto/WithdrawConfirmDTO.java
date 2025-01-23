package ltd.wrb.payment.controller.dto;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;

import lombok.Data;

@Data
public class WithdrawConfirmDTO {

    private Long logId;

    private String txHash;

    private BigInteger blockNumber;

    private Long timestamp;

    private String nonce;

    private String sign;

    public boolean isValidSign() {
        Map<String, Object> params = Map.of("logId", logId, "txHash", txHash, "blockNumber", blockNumber, "timestamp",
                timestamp, "nonce", nonce);
        TreeMap<String, Object> sortedParams = new TreeMap<>(params);
        String base = (String) sortedParams.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b).orElse("");
        String signature = DigestUtils.sha256Hex(base + System.getenv("WITHDRAW_NOTIFY_SECRET"));
        return signature.equals(sign);
    }
}
