package com.eventhub.payment.entity;

public enum PaymentStatus {
    INITIATED,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED
}
