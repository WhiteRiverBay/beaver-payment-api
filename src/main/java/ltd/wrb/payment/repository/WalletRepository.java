package ltd.wrb.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.model.Wallet;

import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Wallet findByUidAndChainType(String uid, ChainType chainType);

    List<Wallet> findByUid(String uid);

    // find by address and chain type
    Wallet findByAddressAndChainType(String address, ChainType chainType);

    // filter by chain type, and exists in deposit log 
    @Query("select w from Wallet w where w.chainType = :chainType and exists (select 1 from TradeLog d where d.txTo = w.address)")
    List<Wallet> findByChainType(ChainType chainType);
}
