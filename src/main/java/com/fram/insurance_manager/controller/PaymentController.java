package com.fram.insurance_manager.controller;

import com.fram.insurance_manager.dto.PaymentDto;
import com.fram.insurance_manager.dto.PaymentUrlDto;
import com.fram.insurance_manager.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("payment")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public PaymentDto save(@Valid @RequestBody PaymentDto payment) {
        return paymentService.save(payment);
    }

    @PostMapping("create-session/{contractId}")
    public PaymentUrlDto createCheckoutSession(@PathVariable UUID contractId) throws Exception {
        return paymentService.createCheckoutSession(contractId);
    }

    @PostMapping("stripe/webhook")
    public void handleStripeWebhook(@RequestBody String payload,
                                    @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleStripeWebhook(payload, sigHeader);

    }
}
