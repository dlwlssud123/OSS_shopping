package com.shopping.engine.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal totalOriginalPrice;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal totalDiscountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    public Order(String idempotencyKey, Customer customer) {
        this.idempotencyKey = idempotencyKey;
        this.customer = customer;
        this.status = OrderStatus.CREATED;
        this.totalOriginalPrice = BigDecimal.ZERO;
        this.totalDiscountAmount = BigDecimal.ZERO;
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
        
        // 가격 재계산
        this.totalOriginalPrice = this.totalOriginalPrice.add(orderItem.getOriginalPrice());
        this.totalDiscountAmount = this.totalDiscountAmount.add(orderItem.getTotalDiscountAmount());
    }

    public BigDecimal calculateFinalPrice() {
        BigDecimal finalPrice = this.totalOriginalPrice.subtract(this.totalDiscountAmount);
        return finalPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalPrice;
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
    }
}
