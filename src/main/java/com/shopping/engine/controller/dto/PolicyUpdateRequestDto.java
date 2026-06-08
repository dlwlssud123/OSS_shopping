package com.shopping.engine.controller.dto;

import java.math.BigDecimal;

public record PolicyUpdateRequestDto(
    Boolean enabled,
    Integer priority,
    Boolean exclusive,
    BigDecimal discountRate,
    BigDecimal discountAmount
) {}
