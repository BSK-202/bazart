package com.marketplace.wallet.dto;

public class PaymentIntentResponse {
    private boolean success;
    private String clientSecret;
    private String paymentIntentId;
    private String message;

    public static PaymentIntentResponse success(String clientSecret, String paymentIntentId) {
        PaymentIntentResponse response = new PaymentIntentResponse();
        response.setSuccess(true);
        response.setClientSecret(clientSecret);
        response.setPaymentIntentId(paymentIntentId);
        response.setMessage("Payment Intent créé avec succès");
        return response;
    }

    public static PaymentIntentResponse error(String message) {
        PaymentIntentResponse response = new PaymentIntentResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    // Getters et Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
