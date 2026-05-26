package com.shopping.engine.policy;

import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.Grade;
import com.shopping.engine.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RateDiscountPolicy implements DiscountPolicy {

    // VIP 이상 10% 할인
    private final BigDecimal discountRate = new BigDecimal("0.10");

    @Override
    public int getPriority() {
        return 1; // 높은 우선순위
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getPolicyName() {
        return "VIP Grade Rate Discount (10%)";
    }

    @Override
    public BigDecimal discount(Customer customer, OrderItem orderItem) {
        if (customer.getGrade() == Grade.VIP || customer.getGrade() == Grade.VVIP) {
            BigDecimal originalPrice = orderItem.getOriginalPrice();
            return originalPrice.multiply(discountRate);
        }
        return BigDecimal.ZERO;
    }
}
