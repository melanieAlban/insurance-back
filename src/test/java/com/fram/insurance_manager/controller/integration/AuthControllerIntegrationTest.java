package com.fram.insurance_manager.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.config.auth.AuthenticationRequest;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.enums.UserRol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerUser_shouldReturnCreatedUser() throws Exception {
        SaveUserDto request = SaveUserDto.builder()
                .name("Integration User")
                .email("integration@example.com")
                .password("password123")
                .rol(UserRol.CLIENT)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void inactiveUser_loginReturns401() throws Exception {
        SaveUserDto inactiveUser = SaveUserDto.builder()
                .name("Inactive")
                .email("inactive@fram.com")
                .password("pwd123")
                .rol(UserRol.CLIENT)
                .active(false)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveUser)))
                .andExpect(status().isOk());

        AuthenticationRequest login = AuthenticationRequest.builder()
                .email("inactive@fram.com")
                .password("pwd123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerAndLoginFlow() throws Exception {
        String email = "integration_" + UUID.randomUUID() + "@test.com";

        SaveUserDto saveUserDto = SaveUserDto.builder()
                .name("Integration User")
                .email(email)
                .password("fakepassword") // será ignorada
                .rol(UserRol.CLIENT)
                .active(true)
                .build();

        // Registro
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // Intento de login fallido con clave que se puso (pero fue ignorada)
        AuthenticationRequest loginRequest = new AuthenticationRequest(email, "fakepassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}