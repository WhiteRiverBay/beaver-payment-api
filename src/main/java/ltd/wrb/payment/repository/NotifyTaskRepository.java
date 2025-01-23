package ltd.wrb.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ltd.wrb.payment.enums.NotifyStatus;
import ltd.wrb.payment.model.NotifyTask;

public interface NotifyTaskRepository extends JpaRepository<NotifyTask, Long> {

    // findByCreatedAtLessThanEqualAndStatus
    List<NotifyTask> findByCreatedAtLessThanEqualAndStatus(Long createdAt, NotifyStatus status);

    // findByCreatedAtGreaterThanAndCreatedAtLessThanEqualAndStatus
    List<NotifyTask> findByCreatedAtGreaterThanAndCreatedAtLessThanEqualAndStatus(Long createdAt, Long createdAt2,
            NotifyStatus status);

}
