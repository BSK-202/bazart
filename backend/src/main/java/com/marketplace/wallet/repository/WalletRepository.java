package com.marketplace.wallet.repository;

import com.marketplace.user.entity.Client;
import com.marketplace.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser(Client user);
}
