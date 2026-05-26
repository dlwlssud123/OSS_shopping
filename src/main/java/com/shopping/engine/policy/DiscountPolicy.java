package com.shopping.engine.policy;

import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.OrderItem;

import java.math.BigDecimal;

public interface DiscountPolicy {
    int getPriority();
    boolean isExclusive();
    String getPolicyName();
    BigDecimal discount(Customer customer, OrderItem orderItem);
}
