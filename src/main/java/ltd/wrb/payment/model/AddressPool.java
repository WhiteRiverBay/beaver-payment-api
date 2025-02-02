package ltd.wrb.payment.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import ltd.wrb.payment.enums.ChainType;

@Entity
@Data
@Table(name = "wrb_address_pool")
public class AddressPool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encrypted_address")
    private String encryptedAddress;

    @Column(name = "used")
    private Boolean used;

    @Column(name = "uid")
    private String uid;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "assigned_at")
    private Date assignedAt;

    @Column
    private ChainType chainType;
}