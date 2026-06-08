package com.shopping.engine.controller.dto;

import java.math.BigDecimal;

public record ProductRequestDto(
    String name,
    BigDecimal price,
    int stockQuantity
) {}
