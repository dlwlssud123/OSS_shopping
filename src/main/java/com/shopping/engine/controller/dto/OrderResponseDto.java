package com.shopping.engine.controller.dto;

import java.math.BigDecimal;

public record OrderResponseDto(
    Long orderId,
    String idempotencyKey,
    String status,
    BigDecimal originalAmount,
    BigDecimal discountAmount,
    BigDecimal finalAmount,
    String receiptId,
    String errorMessage
) {}
