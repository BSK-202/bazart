package com.marketplace.wallet.repository;

import com.marketplace.user.entity.Client;
import com.marketplace.wallet.entity.HistoriqueWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueWalletRepository extends JpaRepository<HistoriqueWallet, Long> {

    List<HistoriqueWallet> findByUserOrderByOperationDateDesc(Client user);

    List<HistoriqueWallet> findByUserAndOperationTypeOrderByOperationDateDesc(Client user, String operationType);
}