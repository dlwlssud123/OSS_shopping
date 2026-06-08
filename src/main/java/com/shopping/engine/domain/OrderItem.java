package com.shopping.engine.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal unitPrice;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiscountInfo> appliedDiscounts = new ArrayList<>();

    protected OrderItem() {
    }

    public OrderItem(Product product, int quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public void addDiscountInfo(DiscountInfo discountInfo) {
        this.appliedDiscounts.add(discountInfo);
        discountInfo.setOrderItem(this);
    }

    public BigDecimal getTotalDiscountAmount() {
        return appliedDiscounts.stream()
                .map(DiscountInfo::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getOriginalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public List<DiscountInfo> getAppliedDiscounts() {
        return appliedDiscounts;
    }

    public void setAppliedDiscounts(List<DiscountInfo> appliedDiscounts) {
        this.appliedDiscounts = appliedDiscounts;
    }
}
