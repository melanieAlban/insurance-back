package com.fram.insurance_manager.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.dto.ClientDto;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.enums.UserRol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ClientDto createSampleClientDto() {
        SaveUserDto userDto = SaveUserDto.builder()
                .name("Integration Client")
                .email("client_integration@example.com")
                .password("password123")
                .rol(UserRol.CLIENT)
                .active(true)
                .build();

        ClientDto clientDto = new ClientDto();
        clientDto.setName("John");
        clientDto.setLastName("Integration");
        clientDto.setIdentificationNumber("INT123456");
        clientDto.setBirthDate(LocalDate.of(1990, 1, 1));
        clientDto.setPhoneNumber("1234567890");
        clientDto.setAddress("123 Integration St");
        clientDto.setGender("Male");
        clientDto.setOccupation("Tester");
        clientDto.setActive(true);
        clientDto.setUser(userDto);

        return clientDto;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createClient_shouldReturnCreatedClient() throws Exception {
        ClientDto clientDto = createSampleClientDto();
        clientDto.setIdentificationNumber("CREATE123");
        String createJson = objectMapper.writeValueAsString(clientDto);

        mockMvc.perform(post("/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.identificationNumber").value("CREATE123"));
    }

    @Test
    void getClient_Unauthorized() throws Exception {
        mockMvc.perform(get("/client"))
                .andExpect(status().isForbidden());
    }
}
