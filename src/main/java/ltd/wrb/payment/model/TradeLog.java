package ltd.wrb.payment.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.enums.TradeType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "wrb_trade_log")
@Entity
public class TradeLog {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column
    private String paymentId;

    @Column(nullable = false)
    @NotNull(message = "uid is required")
    private String uid;

    @Column(nullable = false)
    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @Column
    @Convert(converter = ltd.wrb.payment.model.converter.TradeTypeConverter.class)
    private TradeType type;

    @Column
    @Builder.Default
    private int confirmedBlocks = 0;

    @Column
    @Builder.Default
    private int confirmedBlocksRequired = 0;
    
    @Column
    private String memo;

    @Column(nullable = true, unique = true)
    private String txHash;

    @Column
    private String token;

    @Column
    private ChainType chainType;

    @Column
    private Integer chainId;

    @Column
    private String txFrom;

    @Column
    private String txTo;

    @Column
    private String blockNumber;

    // created at
    @Column(updatable = false)
    @Builder.Default
    private long createdAt = System.currentTimeMillis();

    @Column
    @Builder.Default    
    private int status = 1;

}
