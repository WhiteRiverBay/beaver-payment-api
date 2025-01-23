package ltd.wrb.payment.service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ltd.wrb.payment.enums.ChainType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentTransaction {

    private String hash;

    private String blockNumber;

    private long timestamp;

    private String from;

    private String to;

    private BigDecimal value;

    private ChainType chainType;
}
