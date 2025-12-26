package com.marketplace.wallet.dto;

public class ConfirmPaymentRequest {
    private String paymentIntentId;
    private double amount;

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}