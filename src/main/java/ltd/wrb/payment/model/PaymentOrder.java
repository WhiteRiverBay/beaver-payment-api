package ltd.wrb.payment.model;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.enums.TradeStatus;

@Entity
@Table(name = "wrb_payment_order")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PaymentOrder {

    @Id
    @Size(max = 32, message = "id length must be less than 32")
    private String id;

    @Column(nullable = false)
    @NotNull(message = "uid is required")
    @Size(max = 64, message = "uid length must be less than 64")
    private String uid;

    @Column(nullable = false, unique = true)
    @NotNull(message = "oid is required")
    @Size(max = 64, message = "oid length must be less than 64")
    private String oid;

    @Column(nullable = false)
    @NotNull(message = "amount is required")
    @Min(value = 0, message = "amount must be greater than 0")
    private BigDecimal amount;

    @Column
    @Builder.Default
    private long expiredAt = System.currentTimeMillis() + 30 * 60 * 1000;

    // createdat
    @Column
    @Builder.Default
    private long createdAt = System.currentTimeMillis();

    @Column
    private Long paidAt;

    // updatedat
    @Column
    @Builder.Default
    private Long updatedAt = System.currentTimeMillis();

    @Column
    @NotNull
    @Builder.Default
    private TradeStatus status = TradeStatus.PENDING_PAY;

    // memo
    @Column
    @Size(max = 128, message = "memo length must be less than 128")
    private String memo;

    @Column
    @Size(max = 128, message = "notify url length must be less than 128")
    @NotNull(message = "notify url is required")
    private String notifyUrl;

    @Column
    private String mchId;

    @Column
    private Long closedAt;

    @Column
    @Size(max = 128, message = "redirect url length must be less than 128")
    @NotNull(message = "redirect url is required")
    private String redirectUrl;

    @Column
    @Size(max = 64, message = "locked address length must be less than 64")
    private String lockedAddress;

    @Column
    @Size(max = 200, message = "logo length must be less than 200")
    private String logo;

    @Transient
    @Builder.Default
    private Map<ChainType, String> addresses = Map.of();
}
