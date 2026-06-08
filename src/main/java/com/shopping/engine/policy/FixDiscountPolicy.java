package com.shopping.engine.policy;

import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FixDiscountPolicy implements DiscountPolicy {

    private final DiscountPolicySettings settings;

    @Autowired
    public FixDiscountPolicy(DiscountPolicySettings settings) {
        this.settings = settings;
    }

    public FixDiscountPolicy() {
        this(new DiscountPolicySettings());
    }

    @Override
    public int getPriority() {
        return settings.getFixPolicy().getPriority();
    }

    @Override
    public boolean isExclusive() {
        return settings.getFixPolicy().isExclusive();
    }

    @Override
    public String getPolicyName() {
        return "Fix Discount (" + settings.getFixPolicy().getDiscountAmount().stripTrailingZeros().toPlainString() + " KRW)";
    }

    @Override
    public BigDecimal discount(Customer customer, OrderItem orderItem) {
        if (!settings.getFixPolicy().isEnabled()) {
            return BigDecimal.ZERO;
        }
        if (orderItem.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            return settings.getFixPolicy().getDiscountAmount();
        }
        return BigDecimal.ZERO;
    }
}
