package com.fram.insurance_manager.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.dto.InsuranceDto;
import com.fram.insurance_manager.enums.InsuranceType;
import com.fram.insurance_manager.enums.PaymentPeriod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InsuranceControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private InsuranceDto buildSample() {
        return InsuranceDto.builder()
                .name("Plan Integral")
                .type(InsuranceType.HEALTH)
                .description("Cobertura total")
                .coverage(80_000.0)
                .deductible(100.0)
                .paymentAmount(25.5)
                .paymentPeriod(PaymentPeriod.MONTHLY)
                .active(true)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void fullCrudFlow() throws Exception {
        String createJson = objectMapper.writeValueAsString(buildSample());

        String savedBody = mockMvc.perform(post("/insurance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        InsuranceDto saved = objectMapper.readValue(savedBody, InsuranceDto.class);
        UUID id = saved.getId();
        assertThat(id).isNotNull();

        mockMvc.perform(get("/insurance/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plan Integral"))
                .andExpect(jsonPath("$.coverage").value(80_000.0));

        InsuranceDto patch = saved.toBuilder()
                .coverage(100_000.0)
                .paymentAmount(30.0)
                .build();

        String patchJson = objectMapper.writeValueAsString(patch);

        mockMvc.perform(put("/insurance/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverage").value(100_000.0))
                .andExpect(jsonPath("$.paymentAmount").value(30.0));

        mockMvc.perform(put("/insurance/status/{id}?status=false", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/insurance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/insurance/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/insurance/{id}", id))
                .andExpect(status().isNotFound());
    }
}
