package com.shopping.engine.infrastructure;

import java.math.BigDecimal;

public record PaymentRequest(
    String orderId,
    BigDecimal amount,
    String customerName
) {}
