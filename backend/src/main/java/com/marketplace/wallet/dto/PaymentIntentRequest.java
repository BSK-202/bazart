package com.marketplace.wallet.dto;

public class PaymentIntentRequest {
    private double amount;
    private String currency = "mad";

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}