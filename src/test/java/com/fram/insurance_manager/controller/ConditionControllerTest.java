package com.fram.insurance_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.dto.ConditionDto;
import com.fram.insurance_manager.service.ConditionService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConditionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConditionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConditionService conditionService;

    @MockitoBean
    private com.fram.insurance_manager.config.auth.JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private com.fram.insurance_manager.util.JwtUtil jwtUtil;

    private final UUID conditionId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    private ConditionDto sampleCondition() {
        return ConditionDto.builder()
                .id(conditionId)
                .name("Condición A")
                .description("Requiere evaluación médica")
                .addedPercentage(10)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void save_ShouldReturnSavedCondition() throws Exception {
        ConditionDto condition = sampleCondition();
        given(conditionService.save(any())).willReturn(condition);

        mockMvc.perform(post("/condition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(condition)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Condición A"));
    }

    @Test
    @WithMockUser
    void getAll_ShouldReturnList() throws Exception {
        given(conditionService.getAll()).willReturn(List.of(sampleCondition()));

        mockMvc.perform(get("/condition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Condición A"));
    }

    @Test
    @WithMockUser
    void getById_ShouldReturnCondition() throws Exception {
        given(conditionService.getById(conditionId)).willReturn(sampleCondition());

        mockMvc.perform(get("/condition/{id}", conditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conditionId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_ShouldReturnNoContent() throws Exception {
        doNothing().when(conditionService).delete(conditionId);

        mockMvc.perform(delete("/condition/{id}", conditionId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void getConditionsByClient_ShouldReturnList() throws Exception {
        given(conditionService.getConditionsByClient(clientId)).willReturn(List.of(sampleCondition()));

        mockMvc.perform(get("/condition/client/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conditionId.toString()));
    }

    @Test
    @WithMockUser
    void assignConditionToClient_ShouldReturnUpdatedList() throws Exception {
        given(conditionService.setConditionToClient(clientId, conditionId)).willReturn(List.of(sampleCondition()));

        mockMvc.perform(post("/condition/client/{clientId}/assign/{conditionId}", clientId, conditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Condición A"));
    }

    @Test
    @WithMockUser
    void removeConditionFromClient_ShouldReturnUpdatedList() throws Exception {
        given(conditionService.removeConditionFromClient(clientId, conditionId)).willReturn(List.of());

        mockMvc.perform(delete("/condition/client/{clientId}/remove/{conditionId}", clientId, conditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
