package com.fram.insurance_manager.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fram.insurance_manager.dto.ClientDto;
import com.fram.insurance_manager.dto.ConditionDto;
import com.fram.insurance_manager.dto.SaveUserDto;
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
@Transactional // Asegura que las operaciones en la base de datos se reviertan después de cada prueba
class ConditionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID clientId;
    private UUID conditionId;

    @BeforeEach
    void setup() throws Exception {
        objectMapper.registerModule(new JavaTimeModule());

        // Crear usuario y cliente
        SaveUserDto userDto = SaveUserDto.builder()
                .name("Test Client")
                .email("client@example.com")
                .password("123456")
                .rol(UserRol.CLIENT)
                .active(true)
                .build();

        ClientDto clientDto = new ClientDto();
        clientDto.setName("John");
        clientDto.setLastName("Doe");
        clientDto.setIdentificationNumber("1234567890");
        clientDto.setBirthDate(LocalDate.of(1990, 1, 1));
        clientDto.setPhoneNumber("0999999999");
        clientDto.setAddress("Fake Street 123");
        clientDto.setGender("Male");
        clientDto.setOccupation("Developer");
        clientDto.setActive(true);
        clientDto.setUser(userDto);

        String clientResponse = mockMvc.perform(post("/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientDto))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        clientId = objectMapper.readValue(clientResponse, ClientDto.class).getId();

        // Crear condición
        ConditionDto conditionDto = ConditionDto.builder()
                .name("Hipertensión")
                .description("Presión alta crónica")
                .addedPercentage(10)
                .build();

        String conditionResponse = mockMvc.perform(post("/condition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conditionDto))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        conditionId = objectMapper.readValue(conditionResponse, ConditionDto.class).getId();
    }


    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void assignAndRemoveConditionToClient() throws Exception {
        // Asignar condición a cliente
        mockMvc.perform(post("/condition/client/{clientId}/assign/{conditionId}", clientId, conditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conditionId.toString()))
                .andExpect(jsonPath("$[0].name").value("Hipertensión"));

        // Verificar que el cliente tiene la condición asignada
        mockMvc.perform(get("/condition/client/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(conditionId.toString()));

        // Remover condición del cliente
        mockMvc.perform(delete("/condition/client/{clientId}/remove/{conditionId}", clientId, conditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getConditionUnauthorized() throws Exception {
        mockMvc.perform(get("/condition"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createConditionUnauthorized() throws Exception {
        ConditionDto conditionDto = ConditionDto.builder()
                .name("Unauthorized")
                .description("Sin permisos")
                .addedPercentage(10)
                .build();

        mockMvc.perform(post("/condition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conditionDto)))
                .andExpect(status().isForbidden());
    }
}