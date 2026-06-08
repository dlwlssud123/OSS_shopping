package com.shopping.engine.policy;

import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.DiscountInfo;
import com.shopping.engine.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class PolicyResolver {

    private final List<DiscountPolicy> policies;

    public PolicyResolver(List<DiscountPolicy> policies) {
        this.policies = policies;
    }

    public BigDecimal applyDiscountRules(Customer customer, List<OrderItem> orderItems) {
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        List<DiscountPolicy> orderedPolicies = policies.stream()
                .sorted(Comparator.comparingInt(DiscountPolicy::getPriority))
                .toList();

        for (OrderItem orderItem : orderItems) {
            BigDecimal itemOriginalPrice = orderItem.getOriginalPrice();
            BigDecimal currentDiscountSum = BigDecimal.ZERO;

            for (DiscountPolicy policy : orderedPolicies) {
                BigDecimal remainingLimit = itemOriginalPrice.subtract(currentDiscountSum);
                if (remainingLimit.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal discountValue = policy.discount(customer, orderItem);
                if (discountValue.compareTo(BigDecimal.ZERO) > 0) {
                    if (discountValue.compareTo(remainingLimit) > 0) {
                        discountValue = remainingLimit;
                    }

                    orderItem.addDiscountInfo(new DiscountInfo(policy.getPolicyName(), discountValue));
                    currentDiscountSum = currentDiscountSum.add(discountValue);

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
