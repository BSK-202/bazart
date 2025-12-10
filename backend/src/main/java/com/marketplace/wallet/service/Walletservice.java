package com.marketplace.wallet.service;

import com.marketplace.wallet.entity.Wallet;
import com.marketplace.wallet.entity.HistoriqueWallet;
import com.marketplace.user.entity.Client;
import com.marketplace.user.service.ClientService;
import com.marketplace.wallet.repository.WalletRepository;
import com.marketplace.wallet.repository.HistoriqueWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class Walletservice {
    private final WalletRepository walletRepository;
    private final HistoriqueWalletRepository historiqueWalletRepository;
    private final ClientService clientService;

    public Walletservice(WalletRepository walletRepository,
                         HistoriqueWalletRepository historiqueWalletRepository,
                         ClientService clientService) {
        this.walletRepository = walletRepository;
        this.historiqueWalletRepository = historiqueWalletRepository;
        this.clientService = clientService;
    }

    public Wallet getWalletByUser(Client user) {
        return walletRepository.findByUser(user).orElseGet(() -> {
            Wallet newWallet = new Wallet(user);
            return walletRepository.save(newWallet);
        });
    }

    public Wallet getWalletByUserId(Long userId) {
        Client user = clientService.getClientById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        return getWalletByUser(user);
    }

    public Wallet getWalletByUserEmail(String email) {
        Client user = clientService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email: " + email));
        return getWalletByUser(user);
    }

    @Transactional
    public Wallet rechargeWallet(Client user, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        Wallet wallet = getWalletByUser(user);
        double balanceBefore = wallet.getBalance();

        wallet.setBalance(wallet.getBalance() + amount);
        Wallet savedWallet = walletRepository.save(wallet);

        // Créer l'historique
        createHistory(user, "CREDIT", amount, balanceBefore, savedWallet.getBalance(),
                "Recharge du portefeuille", generateReference());

        return savedWallet;
    }

    @Transactional
    public Wallet debitWallet(Client user, double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        Wallet wallet = getWalletByUser(user);

        if (wallet.getBalance() < amount) {
            throw new IllegalArgumentException("Solde insuffisant");
        }

        double balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance() - amount);
        Wallet savedWallet = walletRepository.save(wallet);

        // Créer l'historique
        createHistory(user, "DEBIT", amount, balanceBefore, savedWallet.getBalance(),
                description, generateReference());

        return savedWallet;
    }

    @Transactional
    public void transfertWallet(Client fromUser, Client toUser, double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        // Débiter l'expéditeur
        Wallet fromWallet = debitWallet(fromUser, amount, "Transfert à " + toUser.getEmail());

        // Créditer le destinataire
        Wallet toWallet = getWalletByUser(toUser);
        double balanceBeforeTo = toWallet.getBalance();
        toWallet.setBalance(toWallet.getBalance() + amount);
        Wallet savedToWallet = walletRepository.save(toWallet);

        // Historique pour le destinataire
        createHistory(toUser, "CREDIT", amount, balanceBeforeTo, savedToWallet.getBalance(),
                "Transfert de " + fromUser.getEmail(), generateReference());
    }

    private void createHistory(Client user, String operationType, double amount,
                               double balanceBefore, double balanceAfter,
                               String description, String reference) {
        HistoriqueWallet history = new HistoriqueWallet(
                user, operationType, amount, balanceBefore, balanceAfter, description, reference
        );
        historiqueWalletRepository.save(history);
    }

    private String generateReference() {
        return "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public List<HistoriqueWallet> getUserHistory(Client user) {
        return historiqueWalletRepository.findByUserOrderByOperationDateDesc(user);
    }

    public List<HistoriqueWallet> getUserHistoryByType(Client user, String operationType) {
        return historiqueWalletRepository.findByUserAndOperationTypeOrderByOperationDateDesc(user, operationType);
    }


}