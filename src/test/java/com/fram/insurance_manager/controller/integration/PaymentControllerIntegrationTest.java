package com.fram.insurance_manager.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fram.insurance_manager.dto.ClientDto;
import com.fram.insurance_manager.dto.ContractDto;
import com.fram.insurance_manager.dto.InsuranceDto;
import com.fram.insurance_manager.dto.PaymentDto;
import com.fram.insurance_manager.dto.PaymentUrlDto;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.enums.ContractStatus;
import com.fram.insurance_manager.enums.InsuranceType;
import com.fram.insurance_manager.enums.PaymentPeriod;
import com.fram.insurance_manager.enums.PaymentType;
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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID contractId;

    @BeforeEach
    void setup() throws Exception {
        // Registrar JavaTimeModule para manejar LocalDate
        objectMapper.registerModule(new JavaTimeModule());

        // Crear usuario y cliente
        SaveUserDto userDto = SaveUserDto.builder()
                .name("Payment Client")
                .email("payment_client@example.com")
                .password("password123")
                .rol(UserRol.CLIENT)
                .active(true)
                .build();

        ClientDto clientDto = new ClientDto();
        clientDto.setName("John");
        clientDto.setLastName("Payment");
        clientDto.setIdentificationNumber("PAYMENT123");
        clientDto.setBirthDate(LocalDate.of(1990, 1, 1));
        clientDto.setPhoneNumber("1234567890");
        clientDto.setAddress("123 Payment St");
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
                .name("Payment Insurance")
                .type(InsuranceType.HEALTH)
                .description("Insurance for payment testing")
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

        // Crear contrato
        ContractDto contractDto = ContractDto.builder()
                .startDate(LocalDate.now())
                .insuranceId(insuranceId)
                .clientId(clientId)
                .status(ContractStatus.ACTIVE) // Suponemos que el contrato debe estar activo para pagos
                .build();

        String contractResponse = mockMvc.perform(post("/contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractDto))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ContractDto savedContract = objectMapper.readValue(contractResponse, ContractDto.class);
        contractId = savedContract.getId();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void savePaymentWithInvalidContractId_shouldReturnNotFound() throws Exception {
        // Crear pago con contrato inexistente
        PaymentDto paymentDto = PaymentDto.builder()
                .amount(199.99)
                .paymentType(PaymentType.CARD)
                .date(LocalDate.now())
                .contractId(UUID.randomUUID()) // Contrato inexistente
                .build();

        mockMvc.perform(post("/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createCheckoutSession_shouldReturnPaymentUrl() throws Exception {
        // Configurar una URL de pago simulada
        // Nota: En un entorno real, esto dependerá de Stripe. Aquí asumimos que el controlador devuelve una URL.
        mockMvc.perform(post("/payment/create-session/{contractId}", contractId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.url").isString());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createCheckoutSessionWithInvalidContractId_shouldReturnNotFound() throws Exception {
        UUID invalidContractId = UUID.randomUUID();
        mockMvc.perform(post("/payment/create-session/{contractId}", invalidContractId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void paymentUnauthorized_shouldReturnForbidden() throws Exception {
        PaymentDto paymentDto = PaymentDto.builder()
                .amount(199.99)
                .paymentType(PaymentType.CARD)
                .date(LocalDate.now())
                .contractId(contractId)
                .build();

        mockMvc.perform(post("/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isForbidden());
    }
}