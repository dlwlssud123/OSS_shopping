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
                    BigDecimal.ZERO,
                    new BigDecimal("0.00"), // basic
                    new BigDecimal("0.10"), // vip
                    new BigDecimal("0.20")  // vvip
            ));
            repository.save(new DiscountPolicySetting(
                    "FIX",
                    "Fix Discount",
                    true,
                    2,
                    true,
                    BigDecimal.ZERO,
                    new BigDecimal("1000.00"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
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
    public PolicySetting updateExtended(String type, Boolean enabled, Integer priority, Boolean exclusive,
                                        BigDecimal discountRate, BigDecimal discountAmount,
                                        BigDecimal basicRate, BigDecimal vipRate, BigDecimal vvipRate) {
        String normalized = normalize(type);
        DiscountPolicySetting setting = repository.findById(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Unknown policy type: " + type));

        boolean nextEnabled = enabled != null ? enabled : setting.isEnabled();
        int nextPriority = priority != null ? priority : setting.getPriority();
        boolean nextExclusive = exclusive != null ? exclusive : setting.isExclusive();
        BigDecimal nextRate = discountRate != null ? discountRate : setting.getDiscountRate();
        BigDecimal nextAmount = discountAmount != null ? discountAmount : setting.getDiscountAmount();
        
        BigDecimal nextBasic = basicRate != null ? basicRate : setting.getBasicDiscountRate();
        BigDecimal nextVip = vipRate != null ? vipRate : setting.getVipDiscountRate();
        BigDecimal nextVvip = vvipRate != null ? vvipRate : setting.getVvipDiscountRate();

        if (nextPriority < 0) {
            throw new IllegalArgumentException("priority must be greater than or equal to zero");
        }
        if (nextRate.signum() < 0 || nextRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discountRate must be between 0 and 1");
        }
        if (nextAmount.signum() < 0) {
            throw new IllegalArgumentException("discountAmount must be greater than or equal to zero");
        }
        if (nextBasic.signum() < 0 || nextBasic.compareTo(BigDecimal.ONE) > 0 ||
            nextVip.signum() < 0 || nextVip.compareTo(BigDecimal.ONE) > 0 ||
            nextVvip.signum() < 0 || nextVvip.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Grade specific discount rates must be between 0 and 1");
        }

        setting.updateExtended(nextEnabled, nextPriority, nextExclusive, nextRate, nextAmount, nextBasic, nextVip, nextVvip);
        DiscountPolicySetting saved = repository.save(setting);
        return mapToDto(saved);
    }

    // 하위 호환성 유지용 update
    @Transactional
    public PolicySetting update(String type, Boolean enabled, Integer priority, Boolean exclusive,
                                BigDecimal discountRate, BigDecimal discountAmount) {
        return updateExtended(type, enabled, priority, exclusive, discountRate, discountAmount, null, null, null);
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
                setting.getDiscountAmount(),
                setting.getBasicDiscountRate(),
                setting.getVipDiscountRate(),
                setting.getVvipDiscountRate()
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
        private final BigDecimal basicDiscountRate;
        private final BigDecimal vipDiscountRate;
        private final BigDecimal vvipDiscountRate;

        private PolicySetting(String type, String name, boolean enabled, int priority, boolean exclusive,
                              BigDecimal discountRate, BigDecimal discountAmount,
                              BigDecimal basicDiscountRate, BigDecimal vipDiscountRate, BigDecimal vvipDiscountRate) {
            this.type = type;
            this.name = name;
            this.enabled = enabled;
            this.priority = priority;
            this.exclusive = exclusive;
            this.discountRate = discountRate;
            this.discountAmount = discountAmount;
            this.basicDiscountRate = basicDiscountRate;
            this.vipDiscountRate = vipDiscountRate;
            this.vvipDiscountRate = vvipDiscountRate;
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

        public BigDecimal getBasicDiscountRate() {
            return basicDiscountRate;
        }

        public BigDecimal getVipDiscountRate() {
            return vipDiscountRate;
        }

        public BigDecimal getVvipDiscountRate() {
            return vvipDiscountRate;
        }
    }
}
