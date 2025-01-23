package ltd.wrb.payment.controller.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

import cn.hutool.core.map.MapUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PaymentDTO {
    private String oid;
    private String uid;
    private BigDecimal amount;
    private String memo;
    private Long expiredAt;
    private Long timestamp;
    private String nonce;
    private String sign;
    private String mchId;
    private String notifyUrl;
    private String redirectUrl;
    private String logo;

    public boolean isValidSign() {
        Map<String, Object> params = new TreeMap<>();
        params.put("oid", oid);
        params.put("uid", uid);
        params.put("amount", amount);
        if (memo != null) {
            params.put("memo", memo);
        }
        params.put("expiredAt", expiredAt);
        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        if (mchId != null) {
            params.put("mchId", mchId);
        }
        if (notifyUrl != null) {
            params.put("notifyUrl", notifyUrl);
        }
        if (redirectUrl != null) {
            params.put("redirectUrl", redirectUrl);
        }
        // logo
        if (logo != null) {
            params.put("logo", logo);
        }

        String base = MapUtil.join(params, "&", "=", true);
        String _sign = DigestUtil.sha256Hex(base + System.getenv("PAYMENT_NOTIFY_SECRET"));
        return _sign.equals(sign);
    }
}
