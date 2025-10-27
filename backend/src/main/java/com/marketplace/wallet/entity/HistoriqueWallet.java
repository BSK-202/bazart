
package com.marketplace.wallet.entity;

import com.marketplace.user.entity.Client;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_history")
public class HistoriqueWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Client user;

    @Column(nullable = false)
    private String operationType; // "CREDIT", "DEBIT", "TRANSFERT"

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private double balanceBefore;

    @Column(nullable = false)
    private double balanceAfter;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime operationDate;

    private String reference; // Référence de transaction

    // Constructeurs
    public HistoriqueWallet() {}

    public HistoriqueWallet(Client user, String operationType, double amount,
                            double balanceBefore, double balanceAfter, String description, String reference) {
        this.user = user;
        this.operationType = operationType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.reference = reference;
        this.operationDate = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Client getUser() { return user; }
    public void setUser(Client user) { this.user = user; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(double balanceBefore) { this.balanceBefore = balanceBefore; }

    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getOperationDate() { return operationDate; }
    public void setOperationDate(LocalDateTime operationDate) { this.operationDate = operationDate; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}