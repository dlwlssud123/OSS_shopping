package com.shopping.engine.infrastructure;

public record PaymentResponse(
    boolean success,
    String receiptId,
    String errorMessage
) {}
