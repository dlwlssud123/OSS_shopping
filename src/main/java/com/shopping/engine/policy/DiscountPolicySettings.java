package com.shopping.engine.policy;

import com.shopping.engine.domain.DiscountPolicySetting;
import com.shopping.engine.repository.DiscountPolicySettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DiscountPolicySettings {

    private final DiscountPolicySettingRepository repository;

    public DiscountPolicySettings(DiscountPolicySettingRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (repository.count() == 0) {
            repository.save(new DiscountPolicySetting(
                    "RATE",
                    "VIP Grade Rate Discount",
                    true,
                    1,
                    false,
                    new BigDecimal("0.10"),
                    BigDecimal.ZERO
            ));
            repository.save(new DiscountPolicySetting(
                    "FIX",
                    "Fix Discount",
                    true,
                    2,
                    true,
                    BigDecimal.ZERO,
                    new BigDecimal("1000.00")
            ));
        }
    }

    @Transactional(readOnly = true)
    public List<PolicySetting> findAll() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PolicySetting getRatePolicy() {
        return getPolicy("RATE");
    }

    @Transactional(readOnly = true)
    public PolicySetting getFixPolicy() {
        return getPolicy("FIX");
    }

    @Transactional(readOnly = true)
    public PolicySetting getPolicy(String type) {
        String normalized = normalize(type);
        DiscountPolicySetting setting = repository.findById(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Unknown policy type: " + type));
        return mapToDto(setting);
    }

    @Transactional
    public PolicySetting update(String type, Boolean enabled, Integer priority, Boolean exclusive,
                                BigDecimal discountRate, BigDecimal discountAmount) {
        String normalized = normalize(type);
        DiscountPolicySetting setting = repository.findById(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Unknown policy type: " + type));

        boolean nextEnabled = enabled != null ? enabled : setting.isEnabled();
        int nextPriority = priority != null ? priority : setting.getPriority();
        boolean nextExclusive = exclusive != null ? exclusive : setting.isExclusive();
        BigDecimal nextRate = discountRate != null ? discountRate : setting.getDiscountRate();
        BigDecimal nextAmount = discountAmount != null ? discountAmount : setting.getDiscountAmount();

        if (nextPriority < 0) {
            throw new IllegalArgumentException("priority must be greater than or equal to zero");
        }
        if (nextRate.signum() < 0 || nextRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discountRate must be between 0 and 1");
        }
        if (nextAmount.signum() < 0) {
            throw new IllegalArgumentException("discountAmount must be greater than or equal to zero");
        }

        setting.update(nextEnabled, nextPriority, nextExclusive, nextRate, nextAmount);
        DiscountPolicySetting saved = repository.save(setting);
        return mapToDto(saved);
    }

    private String normalize(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("policy type is required");
        }
        return type.trim().toUpperCase();
    }

    private PolicySetting mapToDto(DiscountPolicySetting setting) {
        return new PolicySetting(
                setting.getType(),
                setting.getName(),
                setting.isEnabled(),
                setting.getPriority(),
                setting.isExclusive(),
                setting.getDiscountRate(),
                setting.getDiscountAmount()
        );
    }

    public static class PolicySetting {
        private final String type;
        private final String name;
        private final boolean enabled;
        private final int priority;
        private final boolean exclusive;
        private final BigDecimal discountRate;
        private final BigDecimal discountAmount;

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
