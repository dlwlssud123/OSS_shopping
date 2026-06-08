package com.shopping.engine.service;

import com.shopping.engine.controller.dto.ItemDto;
import com.shopping.engine.controller.dto.OrderResponseDto;
import com.shopping.engine.domain.*;
import com.shopping.engine.infrastructure.PaymentGateway;
import com.shopping.engine.infrastructure.PaymentRequest;
import com.shopping.engine.infrastructure.PaymentResponse;
import com.shopping.engine.policy.PolicyResolver;
import com.shopping.engine.repository.CustomerRepository;
import com.shopping.engine.repository.OrderRepository;
import com.shopping.engine.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PolicyResolver policyResolver;
    private final PaymentGateway paymentGateway;

    public OrderService(ProductRepository productRepository, 
                        OrderRepository orderRepository, 
                        CustomerRepository customerRepository, 
                        PolicyResolver policyResolver, 
                        PaymentGateway paymentGateway) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.policyResolver = policyResolver;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public OrderResponseDto processOrder(Long customerId, List<ItemDto> items, String idempotencyKey) {
        log.info("Processing order for customer: {}, idempotencyKey: {}", customerId, idempotencyKey);

        // 1. 멱등성 검증
        Optional<Order> existingOrderOpt = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrderOpt.isPresent()) {
            Order existingOrder = existingOrderOpt.get();
            log.info("Duplicate order request detected. Returning existing order: {}", existingOrder.getOrderId());
            return makeOrderResponseDto(existingOrder, null, null);
        }

        // 2. 고객 조회
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customerId));

        // 3. 주문 객체 생성 및 1차 상품 락킹/정보 조회
        Order order = new Order(idempotencyKey, customer);
        
        for (ItemDto item : items) {
            // 비관적 락 획득하여 상품 정보 조회 (동시성 제어)
            Product product = productRepository.findByIdWithPessimisticLock(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + item.productId()));

            // OrderItem 생성
            OrderItem orderItem = new OrderItem(product, item.quantity(), product.getPrice());
            order.addOrderItem(orderItem);
        }

        // 4. PolicyResolver를 통한 실시간 할인 금액 계산 및 적용
        BigDecimal totalDiscount = policyResolver.applyDiscountRules(customer, order.getOrderItems());
        
        // 5. 총 원가, 총 할인액 업데이트 및 PAYMENT_PENDING으로 상태 명시
        order.setTotalDiscountAmount(totalDiscount);
        order.changeStatus(OrderStatus.PAYMENT_PENDING);

        // 6. 재고 선차감 실행
        try {
            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                product.removeStock(orderItem.getQuantity());
            }
        } catch (com.shopping.engine.exception.NotEnoughStockException e) {
            log.warn("Stock insufficient for order. Marking as FAILED.", e);
            order.changeStatus(OrderStatus.FAILED);
            order = orderRepository.save(order);
            return makeOrderResponseDto(order, null, "재고 부족: " + e.getMessage());
        }

        // 1차 주문 저장 (ID 식별자 발급 및 상태 저장을 위함)
        order = orderRepository.save(order);

        // 7. 외부 PG 결제 승인 요청 전송
        BigDecimal finalPaymentAmount = order.calculateFinalPrice();
        PaymentRequest paymentRequest = new PaymentRequest(
                order.getOrderId().toString(),
                finalPaymentAmount,
                customer.getName()
        );

        PaymentResponse paymentResponse = null;
        try {
            paymentResponse = paymentGateway.approvePayment(paymentRequest);
        } catch (Exception e) {
            log.error("Payment Gateway communication failed", e);
            paymentResponse = new PaymentResponse(false, null, "PG Gateway error: " + e.getMessage());
        }

        // 8. PG 승인 결과 처리 (보상 트랜잭션 구현)
        if (paymentResponse != null && paymentResponse.success()) {
            log.info("Payment approved for order: {}", order.getOrderId());
            order.changeStatus(OrderStatus.COMPLETE);
            order = orderRepository.save(order); // 성공 영속화
            return makeOrderResponseDto(order, paymentResponse.receiptId(), null);
        } else {
            String errorMsg = (paymentResponse != null) ? paymentResponse.errorMessage() : "Unknown PG error";
            log.warn("Payment failed for order: {}, Reason: {}. Triggering compensation transaction.", order.getOrderId(), errorMsg);

            // 보상 트랜잭션: 선차감된 재고 복구
            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                product.addStock(orderItem.getQuantity());
            }

            // 주문 상태 FAILED로 영속화
            order.changeStatus(OrderStatus.FAILED);
            order = orderRepository.save(order); // 실패 이력 영속화
            
            return makeOrderResponseDto(order, null, errorMsg);
        }
    }

    private OrderResponseDto makeOrderResponseDto(Order order, String receiptId, String errorMessage) {
        return new OrderResponseDto(
                order.getOrderId(),
                order.getIdempotencyKey(),
                order.getStatus().name(),
                order.getTotalOriginalPrice(),
                order.getTotalDiscountAmount(),
                order.calculateFinalPrice(),
                receiptId,
                errorMessage
        );
    }
}
