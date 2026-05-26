package com.shopping.engine.controller;

import com.shopping.engine.controller.dto.OrderRequestDto;
import com.shopping.engine.controller.dto.OrderResponseDto;
import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.Grade;
import com.shopping.engine.domain.Product;
import com.shopping.engine.repository.CustomerRepository;
import com.shopping.engine.repository.ProductRepository;
import com.shopping.engine.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*") // GitHub Pages 및 로컬 테스트 CORS 완전 허용
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody OrderRequestDto request) {
        log.info("Received order request: {}", request);
        try {
            OrderResponseDto response = orderService.processOrder(
                    request.customerId(),
                    request.items(),
                    request.idempotencyKey()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Order processing failed with exception", e);
            OrderResponseDto errorResponse = new OrderResponseDto(
                    null,
                    request.idempotencyKey(),
                    "FAILED",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> getCustomers() {
        return ResponseEntity.ok(customerRepository.findAll());
    }

    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> initializeDemoData() {
        log.info("Initializing demo data...");
        
        // 데이터가 없는 경우에만 초기 데이터 삽입
        if (customerRepository.count() == 0) {
            customerRepository.save(new Customer("이진녕 (BASIC)", Grade.BASIC));
            customerRepository.save(new Customer("김철수 (VIP)", Grade.VIP));
            customerRepository.save(new Customer("홍길동 (VVIP)", Grade.VVIP));
        }

        if (productRepository.count() == 0) {
            productRepository.save(new Product("맥북 프로", new BigDecimal("3000000.00"), 5));
            productRepository.save(new Product("아이폰 15", new BigDecimal("1200000.00"), 10));
            productRepository.save(new Product("에어팟 프로", new BigDecimal("300000.00"), 20));
            productRepository.save(new Product("소형 양말 (정액할인 테스트용)", new BigDecimal("1000.00"), 100));
        }

        return ResponseEntity.ok(Map.of("message", "Demo data initialized successfully"));
    }
}
