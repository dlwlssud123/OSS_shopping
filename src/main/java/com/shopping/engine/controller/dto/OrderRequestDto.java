package com.shopping.engine.controller.dto;

import java.util.List;

public record OrderRequestDto(
    Long customerId,
    List<ItemDto> items,
    String idempotencyKey
) {}
