package com.shopping.engine.policy;

import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.Grade;
import com.shopping.engine.domain.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RateDiscountPolicy implements DiscountPolicy {

    private final DiscountPolicySettings settings;

    @Autowired
    public RateDiscountPolicy(DiscountPolicySettings settings) {
        this.settings = settings;
    }

    public RateDiscountPolicy() {
        this(new DiscountPolicySettings());
    }

    @Override
    public int getPriority() {
        return settings.getRatePolicy().getPriority();
    }

    @Override
    public boolean isExclusive() {
        return settings.getRatePolicy().isExclusive();
    }

    @Override
    public String getPolicyName() {
        BigDecimal percent = settings.getRatePolicy().getDiscountRate().multiply(new BigDecimal("100"));
        return "VIP Grade Rate Discount (" + percent.stripTrailingZeros().toPlainString() + "%)";
    }

    @Override
    public BigDecimal discount(Customer customer, OrderItem orderItem) {
        if (!settings.getRatePolicy().isEnabled()) {
            return BigDecimal.ZERO;
        }
        if (customer.getGrade() == Grade.VIP || customer.getGrade() == Grade.VVIP) {
            return orderItem.getOriginalPrice().multiply(settings.getRatePolicy().getDiscountRate());
        }
        return BigDecimal.ZERO;
    }
}
