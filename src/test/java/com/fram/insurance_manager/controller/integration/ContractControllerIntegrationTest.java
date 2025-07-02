package com.fram.insurance_manager.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fram.insurance_manager.dto.*;
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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContractControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID clientId;
    private UUID insuranceId;

    @BeforeEach
    void setup() throws Exception {
        // Registrar JavaTimeModule para manejar LocalDate
        objectMapper.registerModule(new JavaTimeModule());

        // Crear usuario y cliente
        SaveUserDto userDto = SaveUserDto.builder()
                .name("Contract Client")
                .email("contract_client@example.com")
                .password("password123")
                .rol(UserRol.CLIENT)
                .active(true)
                .build();

        ClientDto clientDto = new ClientDto();
        clientDto.setName("John");
        clientDto.setLastName("Contract");
        clientDto.setIdentificationNumber("CONTRACT123");
        clientDto.setBirthDate(LocalDate.of(1990, 1, 1));
        clientDto.setPhoneNumber("1234567890");
        clientDto.setAddress("123 Contract St");
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

        clientId = objectMapper.readValue(clientResponse, ClientDto.class).getId();

        // Crear seguro
        InsuranceDto insuranceDto = InsuranceDto.builder()
                .name("Contract Insurance")
                .type(InsuranceType.HEALTH)
                .description("Insurance for contract testing")
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

        insuranceId = objectMapper.readValue(insuranceResponse, InsuranceDto.class).getId();
    }


    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getContractById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/contract/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getContract_Unauthorized() throws Exception {
        mockMvc.perform(get("/contract"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createContractWithInvalidClientId_ShouldReturnNotFound() throws Exception {
        ContractDto contractDto = ContractDto.builder()
                .startDate(LocalDate.now())
                .insuranceId(insuranceId)
                .clientId(UUID.randomUUID()) // Cliente inexistente
                .build();

        mockMvc.perform(post("/contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void rejectAttachments_ShouldRejectSuccessfully() throws Exception {
        // Crear contrato
        ContractDto contractDto = ContractDto.builder()
                .startDate(LocalDate.now())
                .insuranceId(insuranceId)
                .clientId(clientId)
                .build();

        String contractResponse = mockMvc.perform(post("/contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID contractId = objectMapper.readValue(contractResponse, ContractDto.class).getId();

        // Rechazar adjuntos
        ObservationMessageDto observation = new ObservationMessageDto();
        observation.setObservation("Los archivos no cumplen con los requisitos");

        mockMvc.perform(post("/contract/reject-attachments/{contractId}", contractId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(observation)))
                .andExpect(status().isOk());

        // Verificar estado después de rechazar adjuntos
        mockMvc.perform(get("/contract/{id}", contractId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ContractStatus.PENDING.toString()));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void approveContractWithoutAttachments_ShouldReturnBadRequest() throws Exception {
        // Crear contrato
        ContractDto contractDto = ContractDto.builder()
                .startDate(LocalDate.now())
                .insuranceId(insuranceId)
                .clientId(clientId)
                .build();

        String contractResponse = mockMvc.perform(post("/contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID contractId = objectMapper.readValue(contractResponse, ContractDto.class).getId();

        // Intentar aprobar contrato sin aprobar adjuntos
        mockMvc.perform(post("/contract/approve-contract/{contractId}", contractId))
                .andExpect(status().isBadRequest());
    }
}