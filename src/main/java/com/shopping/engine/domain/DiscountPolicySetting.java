package com.shopping.engine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "discount_policy_settings")
public class DiscountPolicySetting {

    @Id
    @Column(name = "policy_type", nullable = false, length = 10)
    private String type; // "RATE" or "FIX"

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean exclusive;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal discountRate;

    @Column(name = "discount_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal discountAmount;

    // 등급별 할인율 추가 필드 (UC3 확장)
    @Column(name = "basic_discount_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal basicDiscountRate = BigDecimal.ZERO;

    @Column(name = "vip_discount_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal vipDiscountRate = new BigDecimal("0.10");

    @Column(name = "vvip_discount_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal vvipDiscountRate = new BigDecimal("0.20");

    protected DiscountPolicySetting() {
    }

    public DiscountPolicySetting(String type, String name, boolean enabled, int priority, boolean exclusive, 
                                 BigDecimal discountRate, BigDecimal discountAmount) {
        this.type = type;
        this.name = name;
        this.enabled = enabled;
        this.priority = priority;
        this.exclusive = exclusive;
        this.discountRate = discountRate;
        this.discountAmount = discountAmount;
    }

    public DiscountPolicySetting(String type, String name, boolean enabled, int priority, boolean exclusive, 
                                 BigDecimal discountRate, BigDecimal discountAmount,
                                 BigDecimal basicDiscountRate, BigDecimal vipDiscountRate, BigDecimal vvipDiscountRate) {
        this(type, name, enabled, priority, exclusive, discountRate, discountAmount);
        this.basicDiscountRate = basicDiscountRate;
        this.vipDiscountRate = vipDiscountRate;
        this.vvipDiscountRate = vvipDiscountRate;
    }

    public void update(boolean enabled, int priority, boolean exclusive, BigDecimal discountRate, BigDecimal discountAmount) {
        this.enabled = enabled;
        this.priority = priority;
        this.exclusive = exclusive;
        this.discountRate = discountRate;
        this.discountAmount = discountAmount;
    }

    public void updateExtended(boolean enabled, int priority, boolean exclusive, BigDecimal discountRate, BigDecimal discountAmount,
                               BigDecimal basicRate, BigDecimal vipRate, BigDecimal vvipRate) {
        this.update(enabled, priority, exclusive, discountRate, discountAmount);
        this.basicDiscountRate = basicRate;
        this.vipDiscountRate = vipRate;
        this.vvipDiscountRate = vvipRate;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getBasicDiscountRate() {
        return basicDiscountRate;
    }

    public void setBasicDiscountRate(BigDecimal basicDiscountRate) {
        this.basicDiscountRate = basicDiscountRate;
    }

    public BigDecimal getVipDiscountRate() {
        return vipDiscountRate;
    }

    public void setVipDiscountRate(BigDecimal vipDiscountRate) {
        this.vipDiscountRate = vipDiscountRate;
    }

    public BigDecimal getVvipDiscountRate() {
        return vvipDiscountRate;
    }

    public void setVvipDiscountRate(BigDecimal vvipDiscountRate) {
        this.vvipDiscountRate = vvipDiscountRate;
    }
}
