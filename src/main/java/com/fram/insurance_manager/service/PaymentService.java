package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.PaymentDto;
import com.fram.insurance_manager.dto.PaymentUrlDto;

import java.util.UUID;

public interface PaymentService {
    PaymentDto save(PaymentDto paymentDto);

    PaymentUrlDto createCheckoutSession(UUID contractId) throws Exception;

    void handleStripeWebhook(String payload, String sigHeader);
}
