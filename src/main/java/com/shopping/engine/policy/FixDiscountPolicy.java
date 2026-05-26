package com.shopping.engine.policy;

import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FixDiscountPolicy implements DiscountPolicy {

    private final BigDecimal discountAmount = new BigDecimal("1000.00");

    @Override
    public int getPriority() {
        return 2; // RateDiscountPolicy 보다 낮은 우선순위
    }

    @Override
    public boolean isExclusive() {
        return true; // 이 정책이 적용되면 이후 할인 정책 적용을 중단시킴
    }

    @Override
    public String getPolicyName() {
        return "Fix Discount (1,000 KRW)";
    }

    @Override
    public BigDecimal discount(Customer customer, OrderItem orderItem) {
        BigDecimal originalPrice = orderItem.getOriginalPrice();
        // 원가보다 할인액이 클 수는 있지만, PolicyResolver에서 최종 보정 처리됨
        if (originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            return discountAmount;
        }
        return BigDecimal.ZERO;
    }
}
