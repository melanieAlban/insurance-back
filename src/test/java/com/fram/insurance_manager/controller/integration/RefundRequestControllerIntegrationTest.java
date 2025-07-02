package com.fram.insurance_manager.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.ClientDto;
import com.fram.insurance_manager.dto.ContractDto;
import com.fram.insurance_manager.dto.InsuranceDto;
import com.fram.insurance_manager.dto.RefundRequestDto;
import com.fram.insurance_manager.dto.RejectRefundRequestDto;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.enums.AttachmentType;
import com.fram.insurance_manager.enums.ContractStatus;
import com.fram.insurance_manager.enums.InsuranceType;
import com.fram.insurance_manager.enums.PaymentPeriod;
import com.fram.insurance_manager.enums.UserRol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // Revertir cambios en la base de datos después de cada prueba
class RefundRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID contractId;

    @BeforeEach
    void setup() throws Exception {
        // Registrar JavaTimeModule para manejar fechas
        objectMapper.registerModule(new JavaTimeModule());

        // Crear usuario y cliente
        SaveUserDto userDto = SaveUserDto.builder()
                .name("Refund Client")
                .email("refund_client@example.com")
                .password("password123")
                .rol(UserRol.CLIENT)
                .active(true)
                .build();

        ClientDto clientDto = new ClientDto();
        clientDto.setName("John");
        clientDto.setLastName("Refund");
        clientDto.setIdentificationNumber("REFUND123");
        clientDto.setBirthDate(LocalDate.of(1990, 1, 1));
        clientDto.setPhoneNumber("1234567890");
        clientDto.setAddress("123 Refund St");
        clientDto.setGender("Male");
        clientDto.setOccupation("Tester");
        clientDto.setActive(true);
        clientDto.setUser(userDto);

        String clientResponse = mockMvc.perform(post("/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientDto))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ClientDto savedClient = objectMapper.readValue(clientResponse, ClientDto.class);
        UUID clientId = savedClient.getId();

        // Crear seguro
        InsuranceDto insuranceDto = InsuranceDto.builder()
                .name("Refund Insurance")
                .type(InsuranceType.HEALTH)
                .description("Insurance for refund testing")
                .coverage(100000.0)
                .deductible(500.0)
                .paymentAmount(50.0)
                .paymentPeriod(PaymentPeriod.MONTHLY)
                .active(true)
                .build();

        String insuranceResponse = mockMvc.perform(post("/insurance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insuranceDto))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        InsuranceDto savedInsurance = objectMapper.readValue(insuranceResponse, InsuranceDto.class);
        UUID insuranceId = savedInsurance.getId();

        // Crear contrato en estado ACTIVE
        ContractDto contractDto = ContractDto.builder()
                .startDate(LocalDate.now())
                .insuranceId(insuranceId)
                .clientId(clientId)
                .status(ContractStatus.ACTIVE)
                .active(true)
                .build();

        String contractResponse = mockMvc.perform(post("/contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractDto))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Corrección: Usar ContractDto en lugar de ClientDto
        ContractDto savedContract = objectMapper.readValue(contractResponse, ContractDto.class);
        contractId = savedContract.getId();
    }


    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getByIdWithInvalidId_shouldReturnNotFound() throws Exception {
        UUID invalidRefundId = UUID.randomUUID();
        mockMvc.perform(get("/refund-request/{refundRequestId}", invalidRefundId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void saveWithInvalidContractId_shouldReturnNotFound() throws Exception {
        AttachmentDto attachment = AttachmentDto.builder()
                .attachmentType(AttachmentType.REFUND_REQUEST)
                .fileName("recibo.pdf")
                .content("data:application/pdf;base64,XYZ123")
                .build();

        RefundRequestDto refundDto = RefundRequestDto.builder()
                .refundType("Parcial")
                .description("Doble cobro en factura")
                .contractId(UUID.randomUUID()) // Contrato inexistente
                .attachments(List.of(attachment))
                .build();

        mockMvc.perform(post("/refund-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void rejectWithInvalidId_shouldReturnNotFound() throws Exception {
        RejectRefundRequestDto rejectDto = new RejectRefundRequestDto();
        rejectDto.setId(UUID.randomUUID());
        rejectDto.setReason("No aplica");

        mockMvc.perform(post("/refund-request/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void approveWithInvalidId_shouldReturnNotFound() throws Exception {
        UUID invalidRefundId = UUID.randomUUID();
        mockMvc.perform(post("/refund-request/approve/{refundRequestId}", invalidRefundId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllUnauthorized_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/refund-request"))
                .andExpect(status().isForbidden());
    }

}