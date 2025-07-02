package com.fram.insurance_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.config.auth.JwtRequestFilter;
import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.RefundRequestDto;
import com.fram.insurance_manager.dto.RejectRefundRequestDto;
import com.fram.insurance_manager.enums.AttachmentType;
import com.fram.insurance_manager.service.RefundRequestService;
import com.fram.insurance_manager.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RefundRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class RefundRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private RefundRequestService refundRequestService;

    private final UUID refundId = UUID.randomUUID();

    private RefundRequestDto sampleRefund() {
        AttachmentDto attachment = AttachmentDto.builder()
                .id(UUID.randomUUID())
                .attachmentType(AttachmentType.REFUND_REQUEST)
                .fileName("recibo.pdf")
                .content("data:application/pdf;base64,XYZ123")
                .build();

        return RefundRequestDto.builder()
                .id(UUID.randomUUID())
                .refundType("Parcial")
                .description("Doble cobro en factura")
                .contractId(UUID.randomUUID())
                .attachments(List.of(attachment))
                .build();
    }

    @Test
    @WithMockUser
    void getAll_ShouldReturnList() throws Exception {
        given(refundRequestService.getRefundRequests()).willReturn(List.of(sampleRefund()));

        mockMvc.perform(get("/refund-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser
    void getById_ShouldReturnRefund() throws Exception {
        given(refundRequestService.getRefundRequest(refundId)).willReturn(sampleRefund());

        mockMvc.perform(get("/refund-request/{refundRequestId}", refundId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void save_ShouldReturnCreatedRefund() throws Exception {
        RefundRequestDto input = sampleRefund();
        given(refundRequestService.save(any(RefundRequestDto.class))).willReturn(input);

        mockMvc.perform(post("/refund-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void reject_ShouldCallService() throws Exception {
        RejectRefundRequestDto rejectDto = new RejectRefundRequestDto();
        rejectDto.setId(refundId);
        rejectDto.setReason("No aplica");

        doNothing().when(refundRequestService).reject(refundId, "No aplica");

        mockMvc.perform(post("/refund-request/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void approve_ShouldCallService() throws Exception {
        doNothing().when(refundRequestService).approve(refundId);

        mockMvc.perform(post("/refund-request/approve/{refundRequestId}", refundId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void uploadAttachments_ShouldCallService() throws Exception {
        AttachmentDto attachment = AttachmentDto.builder()
                .id(UUID.randomUUID())
                .attachmentType(AttachmentType.PAYMENT_PROOF) // Usa un valor real de tu enum
                .fileName("receipt.pdf")
                .content("data:application/pdf;base64,XYZ123")
                .build();
        List<AttachmentDto> attachments = List.of(attachment);

        doNothing().when(refundRequestService).updateAttachments(eq(refundId), any());

        mockMvc.perform(post("/refund-request/attachments/{refundRequestId}", refundId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attachments)))
                .andExpect(status().isOk());
    }
}
