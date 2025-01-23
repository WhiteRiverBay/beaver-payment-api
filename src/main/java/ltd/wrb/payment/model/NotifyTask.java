package ltd.wrb.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import ltd.wrb.payment.enums.NotifyStatus;

@Entity
@Table(name = "wrb_notify_task")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@lombok.Data
public class NotifyTask {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column
    private String url;

    @Column
    private String data;

    @Column(unique = true)
    private String paymentId;

    @Column
    private String oid;

    @Column
    private String uid;

    @Column
    private int retryCount;

    @Column
    private NotifyStatus status;

    @Column
    private long nextNotifyTime;

    @Column
    @Builder.Default
    private long createdAt = System.currentTimeMillis();

    @Column
    private long updatedAt;

    @Column
    private Long executedAt;

    @Column
    private String executor;
}
