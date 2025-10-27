package com.marketplace.wallet.entity;

import com.marketplace.user.entity.Client;
import jakarta.persistence.*;

@Entity
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double balance = 0.0;

    @OneToOne
    @JoinColumn(name = "user_id")
    private Client user;

    public Wallet() {}

    public Wallet(Client  user) {
        this.user = user;
        this.balance = 0.0;
    }

    public Long getId() { return id; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public Client  getUser() { return user; }
    public void setUser(Client  user) { this.user = user; }
}
