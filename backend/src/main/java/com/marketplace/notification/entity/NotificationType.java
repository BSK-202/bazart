package com.marketplace.notification.entity;

public enum NotificationType {
    PRODUCT_ACCEPTED,
    PRODUCT_REFUSED,
    AUCTION_START,
    NEW_BID,
    OUTBID,
    AUCTION_WON,
    PRODUCT_DELIVERED,
    PRODUCT_SOLD,
    MESSAGE,
    ACCOUNT_UPDATED,
    PAYMENT_RECEIVED,
    PAYMENT_SENT,
    ADMIN_ALERT,
    GENERIC,
    AUCTION_END,
    PRODUCT_EXPERTISE_REQUIRED,     // produit accepté mais expertise à planifier
    PRODUCT_EXPERTISE_PLANNED ,
    TRANSACTION_ACCEPTED,
    TRANSACTION_ANNULEE,
    AUCTION_END_NO_WINNER// (plus tard) quand la date est vraiment planifiée
}