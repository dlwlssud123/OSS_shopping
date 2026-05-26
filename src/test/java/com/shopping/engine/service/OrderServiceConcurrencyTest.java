package com.shopping.engine.service;

import com.shopping.engine.controller.dto.ItemDto;
import com.shopping.engine.controller.dto.OrderResponseDto;
import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.Grade;
import com.shopping.engine.domain.OrderStatus;
import com.shopping.engine.domain.Product;
import com.shopping.engine.repository.CustomerRepository;
import com.shopping.engine.repository.OrderRepository;
import com.shopping.engine.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Product targetProduct;
    private Customer customer;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();

        // 1. 테스트용 고객 생성 (BASIC 등급)
        customer = customerRepository.save(new Customer("TestCustomer", Grade.BASIC));

        // 2. 테스트용 상품 생성 (재고: 10개)
        targetProduct = productRepository.save(new Product("LimitedEdition", new BigDecimal("5000.00"), 10));
    }

    @Test
    @DisplayName("동시에 30개의 결제 요청이 오더라도, 비관적 락으로 재고 10개만큼만 성공하고 최종 재고는 0이 된다")
    void concurrencyPessimisticLockTest() throws InterruptedException {
        int numberOfThreads = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    String uniqueIdempotencyKey = "CONCUR-" + UUID.randomUUID().toString();
                    ItemDto itemDto = new ItemDto(targetProduct.getProductId(), 1);
                    
                    OrderResponseDto response = orderService.processOrder(
                            customer.getId(),
                            Collections.singletonList(itemDto),
                            uniqueIdempotencyKey
                    );

                    if ("COMPLETE".equals(response.status())) {
                        successCount.incrementAndGet();
                    } else if ("FAILED".equals(response.status())) {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Product updatedProduct = productRepository.findById(targetProduct.getProductId()).orElseThrow();
        
        System.out.println("### Success Count: " + successCount.get());
        System.out.println("### Fail Count: " + failCount.get());
        System.out.println("### Final Stock Quantity: " + updatedProduct.getStockQuantity());

        // 10개 재고 중 10개 결제 성공 보장
        assertThat(successCount.get()).isEqualTo(10);
        // 나머지 20개는 재고 부족으로 FAILED 처리됨
        assertThat(failCount.get()).isEqualTo(20);
        // 최종 재고는 0
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);

        // COMPLETE 상태의 주문 이력이 정확히 10개인지 검증
        long completeOrderCount = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETE)
                .count();
        assertThat(completeOrderCount).isEqualTo(10);
        
        // FAILED 상태의 주문 이력이 정확히 20개 기록되어 있는지 검증 (보상 트랜잭션 정상 수행 증적)
        long failedOrderCount = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.FAILED)
                .count();
        assertThat(failedOrderCount).isEqualTo(20);
    }
}
