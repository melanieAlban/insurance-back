package com.fram.insurance_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.config.auth.JwtRequestFilter;
import com.fram.insurance_manager.dto.PaymentDto;
import com.fram.insurance_manager.dto.PaymentUrlDto;
import com.fram.insurance_manager.enums.PaymentType;
import com.fram.insurance_manager.service.PaymentService;
import com.fram.insurance_manager.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    private PaymentDto samplePayment() {
        return PaymentDto.builder()
                .id(UUID.randomUUID())
                .amount(199.99)
                .contractId(UUID.randomUUID())
                .date(LocalDate.now())
                .paymentType(PaymentType.CARD)
                .build();
    }

    @Test
    @WithMockUser
    void save_ShouldReturnSavedPayment() throws Exception {
        PaymentDto payment = samplePayment();
        given(paymentService.save(any(PaymentDto.class))).willReturn(payment);

        mockMvc.perform(post("/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(payment.getId().toString()))
                .andExpect(jsonPath("$.amount").value(payment.getAmount()));
    }

    @Test
    @WithMockUser
    void createCheckoutSession_ShouldReturnPaymentUrl() throws Exception {
        UUID contractId = UUID.randomUUID();
        PaymentUrlDto urlDto = new PaymentUrlDto();
        urlDto.setUrl("https://checkout.stripe.com/pay/session123");

        given(paymentService.createCheckoutSession(eq(contractId))).willReturn(urlDto);

        mockMvc.perform(post("/payment/create-session/{contractId}", contractId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/pay/session123"));
    }

    @Test
    @WithMockUser
    void handleStripeWebhook_ShouldReturnOk() throws Exception {
        String payload = "{\"id\": \"evt_test_webhook\"}";
        String sigHeader = "t=timestamp,v1=signature";

        doNothing().when(paymentService).handleStripeWebhook(payload, sigHeader);

        mockMvc.perform(post("/payment/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
