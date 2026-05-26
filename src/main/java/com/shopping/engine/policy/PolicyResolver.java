package com.shopping.engine.policy;

import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.DiscountInfo;
import com.shopping.engine.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PolicyResolver {

    private final List<DiscountPolicy> policies;

    public PolicyResolver(List<DiscountPolicy> policies) {
        // 우선순위가 낮은(Priority 값이 작은) 순서대로 정렬하여 저장
        this.policies = policies.stream()
                .sorted(Comparator.comparingInt(DiscountPolicy::getPriority))
                .collect(Collectors.toList());
    }

    public BigDecimal applyDiscountRules(Customer customer, List<OrderItem> orderItems) {
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        for (OrderItem orderItem : orderItems) {
            BigDecimal itemOriginalPrice = orderItem.getOriginalPrice();
            BigDecimal currentDiscountSum = BigDecimal.ZERO;

            for (DiscountPolicy policy : policies) {
                // 적용 가능한 남은 할인 한도 = 상품 총액 - 지금까지 적용된 할인액
                BigDecimal remainingLimit = itemOriginalPrice.subtract(currentDiscountSum);
                if (remainingLimit.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal discountValue = policy.discount(customer, orderItem);
                if (discountValue.compareTo(BigDecimal.ZERO) > 0) {
                    // 한도 초과 시 한도만큼만 적용
                    if (discountValue.compareTo(remainingLimit) > 0) {
                        discountValue = remainingLimit;
                    }

                    if (discountValue.compareTo(BigDecimal.ZERO) > 0) {
                        orderItem.addDiscountInfo(new DiscountInfo(policy.getPolicyName(), discountValue));
                        currentDiscountSum = currentDiscountSum.add(discountValue);
                    }
                    
                    // 배타적(exclusive) 정책인 경우 후속 정책 적용 중단
                    if (policy.isExclusive()) {
                        break;
                    }
                }
            }
            totalDiscountAmount = totalDiscountAmount.add(currentDiscountSum);
        }

        return totalDiscountAmount;
    }
}
