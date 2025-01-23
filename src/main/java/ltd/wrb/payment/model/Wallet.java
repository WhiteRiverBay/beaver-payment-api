package ltd.wrb.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ltd.wrb.payment.enums.ChainType;

@Data
@Table(name = "wrb_wallet")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @Column(name = "chain_type", updatable = false) // @@@
    @NotNull
    private ChainType chainType;

    @Column(updatable = false, nullable = false)
    @NotNull
    private String address;

    @Column(updatable = false, nullable = false, name = "ecryped_private_key")
    @NotNull
    private String encryptedPrivateKey;

    @Column(updatable = false, nullable = false)
    @NotNull
    private String encryptedAesKey;

    @Column(updatable = false, nullable = false)
    @NotNull
    private String uid;
}
