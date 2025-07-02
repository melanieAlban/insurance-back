package com.fram.insurance_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.config.auth.JwtRequestFilter;
import com.fram.insurance_manager.dto.InsuranceDto;
import com.fram.insurance_manager.entity.Benefit;
import com.fram.insurance_manager.enums.InsuranceType;
import com.fram.insurance_manager.enums.PaymentPeriod;
import com.fram.insurance_manager.service.InsuranceService;
import com.fram.insurance_manager.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InsuranceController.class)
@AutoConfigureMockMvc(addFilters = false)
class InsuranceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsuranceService insuranceService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID insuranceId = UUID.randomUUID();

    private InsuranceDto sampleDto() {
        return InsuranceDto.builder()
                .id(insuranceId)
                .name("Plan Salud Familiar")
                .type(InsuranceType.HEALTH)
                .description("Cobertura completa familiar")
                .coverage(100_000.0)
                .deductible(200.0)
                .paymentAmount(59.99)
                .paymentPeriod(PaymentPeriod.MONTHLY)
                .active(true)
                .build();
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_ShouldReturnInsuranceList() throws Exception {
        given(insuranceService.getAll()).willReturn(List.of(sampleDto()));

        mockMvc.perform(get("/insurance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_WhenExists_ShouldReturnInsurance() throws Exception {
        given(insuranceService.getById(insuranceId)).willReturn(sampleDto());

        mockMvc.perform(get("/insurance/{id}", insuranceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(insuranceId.toString()))
                .andExpect(jsonPath("$.name").value("Plan Salud Familiar"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void save_ShouldCreateInsurance() throws Exception {
        InsuranceDto toSave = sampleDto();
        given(insuranceService.save(any(InsuranceDto.class))).willReturn(toSave);

        mockMvc.perform(post("/insurance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toSave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plan Salud Familiar"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_ShouldModifyInsurance() throws Exception {
        InsuranceDto updated = sampleDto().toBuilder()
                .name("Plan Salud Premium")
                .coverage(150_000.0)
                .build();

        given(insuranceService.update(eq(insuranceId), any(InsuranceDto.class)))
                .willReturn(updated);

        mockMvc.perform(put("/insurance/{id}", insuranceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plan Salud Premium"))
                .andExpect(jsonPath("$.coverage").value(150_000.0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateActiveStatus_ShouldToggleStatus() throws Exception {
        InsuranceDto inactive = sampleDto().toBuilder().active(false).build();
        given(insuranceService.updateStatusById(insuranceId, false)).willReturn(inactive);

        mockMvc.perform(put("/insurance/status/{id}", insuranceId)
                        .param("status", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_ShouldRemoveInsurance() throws Exception {
        doNothing().when(insuranceService).delete(insuranceId);

        mockMvc.perform(delete("/insurance/{id}", insuranceId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getBenefits_ShouldReturnBenefitList() throws Exception {
        Benefit mockBenefit = new Benefit();
        mockBenefit.setName("Asistencia Médica Internacional");

        given(insuranceService.getBenefits()).willReturn(List.of(mockBenefit));

        mockMvc.perform(get("/insurance/benefits"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Asistencia Médica Internacional"));
    }
}
