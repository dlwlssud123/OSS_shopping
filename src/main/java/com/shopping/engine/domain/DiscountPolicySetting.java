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

    public void update(boolean enabled, int priority, boolean exclusive, BigDecimal discountRate, BigDecimal discountAmount) {
        this.enabled = enabled;
        this.priority = priority;
        this.exclusive = exclusive;
        this.discountRate = discountRate;
        this.discountAmount = discountAmount;
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
}
