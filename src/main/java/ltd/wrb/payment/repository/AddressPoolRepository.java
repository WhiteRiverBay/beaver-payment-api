package ltd.wrb.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.model.AddressPool;

public interface AddressPoolRepository extends JpaRepository<AddressPool, Long> {

    @Query("SELECT a FROM AddressPool a WHERE a.used = false AND a.chainType = :chainType ORDER BY a.id ASC LIMIT 1")
    Optional<AddressPool> findFirstByUsedFalseAndChainType(ChainType chainType);
}
