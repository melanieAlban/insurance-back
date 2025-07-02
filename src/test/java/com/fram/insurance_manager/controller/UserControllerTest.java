package com.fram.insurance_manager.controller;

import com.fram.insurance_manager.config.auth.JwtRequestFilter;
import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID userId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_ShouldReturnUsers() throws Exception {
        UserDto dto = UserDto.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .active(true)
                .build();

        given(userService.getAll()).willReturn(List.of(dto));

        mockMvc.perform(get("/user"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_WhenUserExists() throws Exception {
        UserDto dto = UserDto.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .active(true)
                .build();
        given(userService.getById(userId)).willReturn(dto);

        mockMvc.perform(get("/user/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void update_ShouldUpdateUser() throws Exception {

        UserDto updated = UserDto.builder()
                .id(userId)
                .name("Updated User")
                .email("updated@example.com")
                .active(false)
                .build();

        given(userService.update(eq(userId), any(UserDto.class)))
                .willReturn(updated);

        mockMvc.perform(put("/user/{id}", userId)   // ← ruta correcta
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_ShouldDeleteUser() throws Exception {
        doNothing().when(userService).delete(userId);

        mockMvc.perform(delete("/user/{id}", userId))
                .andExpect(status().isOk());
    }
}