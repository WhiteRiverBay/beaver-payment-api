package ltd.wrb.payment.controller.dto;

import java.math.BigDecimal;
import java.util.Map;

import cn.hutool.core.map.MapUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundDTO {

    private String id;

    private String oid;

    private BigDecimal amount;

    private String remark;

    private String sign;

    private long timestamp;

    private String nonce;

    public boolean isValidSign() {
        Map<String, Object> map = new java.util.TreeMap<>();
        if (id != null) {
            map.put("id", id);
        }
        if (oid != null) {
            map.put("oid", oid);
        }
        map.put("amount", amount);
        if (remark != null) {
            map.put("remark", remark);
        }
        map.put("timestamp", timestamp);
        map.put("nonce", nonce);

        String base = MapUtil.join(map, "&", "=", true);
        String _sign = DigestUtil.sha256Hex(base + System.getenv("PAYMENT_NOTIFY_SECRET"));
        return _sign.equals(sign);
    }

}
