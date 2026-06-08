package com.shopping.engine.infrastructure;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResponse approvePayment(PaymentRequest request) {
        // 모의 딜레이
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 특정 결제 실패 조건 설정
        if ("FAIL_USER".equals(request.customerName())) {
            return new PaymentResponse(false, null, "Card limit exceeded (잔액 부족)");
        }
        
        if (request.amount().compareTo(new BigDecimal("10000000")) >= 0) {
            return new PaymentResponse(false, null, "Invalid payment amount limit (10,000,000 KRW limit)");
        }

        // 성공 응답
        String receiptId = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResponse(true, receiptId, null);
    }
}
