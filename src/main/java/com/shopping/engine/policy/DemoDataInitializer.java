package com.shopping.engine.policy;

import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.Grade;
import com.shopping.engine.domain.Product;
import com.shopping.engine.repository.CustomerRepository;
import com.shopping.engine.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public DemoDataInitializer(CustomerRepository customerRepository, ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (customerRepository.count() == 0) {
            log.info("Initializing default customers for simulation...");
            customerRepository.save(new Customer("이진녕 (BASIC)", Grade.BASIC));
            customerRepository.save(new Customer("김철수 (VIP)", Grade.VIP));
            customerRepository.save(new Customer("홍길동 (VVIP)", Grade.VVIP));
            customerRepository.save(new Customer("FAIL_USER", Grade.BASIC));
        }

        if (productRepository.count() == 0) {
            log.info("Initializing default products for simulation...");
            productRepository.save(new Product("맥북 프로", new BigDecimal("3000000.00"), 5));
            productRepository.save(new Product("아이폰 15", new BigDecimal("1200000.00"), 10));
            productRepository.save(new Product("에어팟 프로", new BigDecimal("300000.00"), 20));
            productRepository.save(new Product("소형 양말 (정액할인 테스트용)", new BigDecimal("1000.00"), 100));
        }
    }
}
