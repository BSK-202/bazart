package com.marketplace.wallet.dto;

import java.util.Map;
import java.util.HashMap;

public class WalletResponse {
    private boolean success;
    private String message;
    private Map<String, Object> data;

    public WalletResponse() {
        this.data = new HashMap<>();
    }

    public static WalletResponse success(String message) {
        WalletResponse response = new WalletResponse();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    public static WalletResponse error(String message) {
        WalletResponse response = new WalletResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    public WalletResponse addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    // Getters et Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}