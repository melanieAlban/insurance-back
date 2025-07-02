package com.fram.insurance_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.config.auth.JwtRequestFilter;
import com.fram.insurance_manager.dto.ContractDto;
import com.fram.insurance_manager.dto.ObservationMessageDto;
import com.fram.insurance_manager.enums.ContractStatus;
import com.fram.insurance_manager.service.ContractService;
import com.fram.insurance_manager.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContractController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContractService contractService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    private ContractDto sampleDto() {
        return ContractDto.builder()
                .id(UUID.randomUUID())
                .startDate(LocalDate.now())
                .status(ContractStatus.ACTIVE)
                .totalPaymentAmount(BigDecimal.valueOf(1000))
                .insuranceId(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .build();
    }

    @Test
    @WithMockUser
    void getAll_ShouldReturnContractList() throws Exception {
        // Arrange
        List<ContractDto> contracts = List.of(sampleDto(), sampleDto());
        given(contractService.findAll()).willReturn(contracts);

        // Act & Assert
        mockMvc.perform(get("/contract"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void getById_WhenExists_ShouldReturnContract() throws Exception {
        // Arrange
        UUID contractId = UUID.randomUUID();
        ContractDto contractDto = sampleDto();
        contractDto.setId(contractId);
        given(contractService.findById(contractId)).willReturn(contractDto);

        // Act & Assert
        mockMvc.perform(get("/contract/{id}", contractId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(contractId.toString()));
    }

    @Test
    @WithMockUser
    void save_ShouldCreateContract() throws Exception {
        // Arrange
        ContractDto inputDto = sampleDto();
        inputDto.setId(null); // New contract doesn't have ID yet
        ContractDto savedDto = sampleDto();
        given(contractService.save(any(ContractDto.class))).willReturn(savedDto);

        // Act & Assert
        mockMvc.perform(post("/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser
    void getById_WhenNotExists_ShouldReturn404() throws Exception {
        // Arrange
        UUID contractId = UUID.randomUUID();
        given(contractService.findById(contractId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el contrato"));

        // Act & Assert
        mockMvc.perform(get("/contract/{id}", contractId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getAllContractData_ShouldReturnContractData() throws Exception {
        // Arrange
        UUID contractId = UUID.randomUUID();
        ContractDto contractDto = sampleDto();
        contractDto.setId(contractId);
        given(contractService.getAllContractData(contractId)).willReturn(contractDto);

        // Act & Assert
        mockMvc.perform(get("/contract/data/{contractId}", contractId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(contractId.toString()));
    }

    @Test
    @WithMockUser
    void approveAttachments_ShouldApproveAttachments() throws Exception {
        // Arrange
        UUID contractId = UUID.randomUUID();
        doNothing().when(contractService).approveAttachments(contractId);

        // Act & Assert
        mockMvc.perform(post("/contract/approve-attachments/{contractId}", contractId))
                .andExpect(status().isOk());

        verify(contractService).approveAttachments(contractId);
    }

    @Test
    @WithMockUser
    void approveContract_ShouldApproveContract() throws Exception {
        // Arrange
        UUID contractId = UUID.randomUUID();
        doNothing().when(contractService).approveContract(contractId);

        // Act & Assert
        mockMvc.perform(post("/contract/approve-contract/{contractId}", contractId))
                .andExpect(status().isOk());

        verify(contractService).approveContract(contractId);
    }

    @Test
    @WithMockUser
    void approvePayment_ShouldApprovePayment() throws Exception {
        // Arrange
        UUID contractId = UUID.randomUUID();
        doNothing().when(contractService).approvePayment(contractId);

        // Act & Assert
        mockMvc.perform(post("/contract/approve-payment/{contractId}", contractId))
                .andExpect(status().isOk());

        verify(contractService).approvePayment(contractId);
    }

    @Test
    @WithMockUser
    void approveContract_WhenNotAllStepsCompleted_ShouldReturn400() throws Exception {
        // Arrange
        UUID contractId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo aprobar el contrato, faltan documentos para continuar"))
                .when(contractService).approveContract(contractId);

        // Act & Assert
        mockMvc.perform(post("/contract/approve-contract/{contractId}", contractId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void rejectAttachments_ShouldRejectSuccessfully() throws Exception {
        // Arrange
        UUID contractId = UUID.randomUUID();
        ObservationMessageDto observation = new ObservationMessageDto();
        observation.setObservation("Los archivos no cumplen con los requisitos");

        doNothing().when(contractService).rejectAttachments(eq(contractId), any(ObservationMessageDto.class));

        // Act & Assert
        mockMvc.perform(post("/contract/reject-attachments/{contractId}", contractId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(observation)))
                .andExpect(status().isOk());

        verify(contractService).rejectAttachments(eq(contractId), any(ObservationMessageDto.class));
    }
}