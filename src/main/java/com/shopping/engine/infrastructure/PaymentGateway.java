package com.shopping.engine.infrastructure;

public interface PaymentGateway {
    PaymentResponse approvePayment(PaymentRequest request);
}
