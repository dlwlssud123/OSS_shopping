package com.shopping.engine.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "discount_info")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscountInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String policyName;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal discountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    public DiscountInfo(String policyName, BigDecimal discountAmount) {
        this.policyName = policyName;
        this.discountAmount = discountAmount;
    }
}
