package com.fram.insurance_manager.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.enums.UserRol;
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
class UserControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private SaveUserDto sampleSaveDto() {
        return SaveUserDto.builder()
                .name("Integration User")
                .email("iuser@fram.com")
                .password("pwd123")
                .rol(UserRol.ADMIN)
                .active(true)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void fullCrudFlow() throws Exception {
        String createJson = objectMapper.writeValueAsString(sampleSaveDto());

        String createdBody = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UserDto created = objectMapper.readValue(createdBody, UserDto.class);
        UUID id = created.getId();
        assertThat(id).isNotNull();

        mockMvc.perform(get("/user/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("iuser@fram.com"));

        UserDto patch = created.toBuilder().name("Updated Name").active(false).build();
        String patchJson = objectMapper.writeValueAsString(patch);

        mockMvc.perform(put("/user/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/user"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/user/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/user/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUser_Unauthorized() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isForbidden());
    }

}
