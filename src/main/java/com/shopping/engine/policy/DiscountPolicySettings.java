package com.shopping.engine.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DiscountPolicySettings {

    private final PolicySetting ratePolicy = new PolicySetting(
            "RATE",
            "VIP Grade Rate Discount",
            true,
            1,
            false,
            new BigDecimal("0.10"),
            BigDecimal.ZERO
    );

    private final PolicySetting fixPolicy = new PolicySetting(
            "FIX",
            "Fix Discount",
            true,
            2,
            true,
            BigDecimal.ZERO,
            new BigDecimal("1000.00")
    );

    public List<PolicySetting> findAll() {
        return List.of(ratePolicy.copy(), fixPolicy.copy());
    }

    public PolicySetting getRatePolicy() {
        return ratePolicy;
    }

    public PolicySetting getFixPolicy() {
        return fixPolicy;
    }

    public PolicySetting getPolicy(String type) {
        String normalized = normalize(type);
        if ("RATE".equals(normalized)) {
            return ratePolicy;
        }
        if ("FIX".equals(normalized)) {
            return fixPolicy;
        }
        throw new IllegalArgumentException("Unknown policy type: " + type);
    }

    public PolicySetting update(String type, Boolean enabled, Integer priority, Boolean exclusive,
                                BigDecimal discountRate, BigDecimal discountAmount) {
        PolicySetting setting = getPolicy(type);
        if (enabled != null) {
            setting.enabled = enabled;
        }
        if (priority != null) {
            if (priority < 0) {
                throw new IllegalArgumentException("priority must be greater than or equal to zero");
            }
            setting.priority = priority;
        }
        if (exclusive != null) {
            setting.exclusive = exclusive;
        }
        if (discountRate != null) {
            if (discountRate.signum() < 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("discountRate must be between 0 and 1");
            }
            setting.discountRate = discountRate;
        }
        if (discountAmount != null) {
            if (discountAmount.signum() < 0) {
                throw new IllegalArgumentException("discountAmount must be greater than or equal to zero");
            }
            setting.discountAmount = discountAmount;
        }
        return setting.copy();
    }

    private String normalize(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("policy type is required");
        }
        return type.trim().toUpperCase();
    }

    public static class PolicySetting {
        private final String type;
        private final String name;
        private boolean enabled;
        private int priority;
        private boolean exclusive;
        private BigDecimal discountRate;
        private BigDecimal discountAmount;

        private PolicySetting(String type, String name, boolean enabled, int priority, boolean exclusive,
                              BigDecimal discountRate, BigDecimal discountAmount) {
            this.type = type;
            this.name = name;
            this.enabled = enabled;
            this.priority = priority;
            this.exclusive = exclusive;
            this.discountRate = discountRate;
            this.discountAmount = discountAmount;
        }

        private PolicySetting copy() {
            return new PolicySetting(type, name, enabled, priority, exclusive, discountRate, discountAmount);
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isExclusive() {
            return exclusive;
        }

        public BigDecimal getDiscountRate() {
            return discountRate;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }
    }
}
