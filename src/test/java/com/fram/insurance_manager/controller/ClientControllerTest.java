package com.fram.insurance_manager.controller;

import com.fram.insurance_manager.config.auth.JwtRequestFilter;
import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.ClientDto;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.enums.AttachmentType;
import com.fram.insurance_manager.enums.UserRol;
import com.fram.insurance_manager.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fram.insurance_manager.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID clientId;
    private ClientDto clientDto;
    private SaveUserDto userDto;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();

        userDto = SaveUserDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .password("password123")
                .rol(UserRol.CLIENT)
                .active(true)
                .build();

        clientDto = new ClientDto();
        clientDto.setId(clientId);
        clientDto.setName("John");
        clientDto.setLastName("Doe");
        clientDto.setIdentificationNumber("123456789");
        clientDto.setBirthDate(LocalDate.of(1990, 1, 1));
        clientDto.setPhoneNumber("1234567890");
        clientDto.setAddress("123 Main St");
        clientDto.setGender("Male");
        clientDto.setOccupation("Engineer");
        clientDto.setActive(true);
        clientDto.setUser(userDto);
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "AGENT"})
    void save_ShouldCreateClient() throws Exception {
        given(clientService.save(any(ClientDto.class))).willReturn(clientDto);

        mockMvc.perform(post("/client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(clientService).save(any(ClientDto.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "AGENT"})
    void update_ShouldUpdateClient() throws Exception {
        ClientDto updatedDto = new ClientDto();
        updatedDto.setId(clientId);
        updatedDto.setName("John Updated");
        updatedDto.setLastName("Doe Updated");
        updatedDto.setIdentificationNumber("123456789");
        updatedDto.setBirthDate(LocalDate.of(1990, 1, 1));
        updatedDto.setPhoneNumber("1234567890");
        updatedDto.setAddress("123 Main St");
        updatedDto.setGender("Male");
        updatedDto.setOccupation("Engineer");
        updatedDto.setActive(true);
        updatedDto.setUser(userDto);

        given(clientService.update(eq(clientId), any(ClientDto.class))).willReturn(updatedDto);

        mockMvc.perform(put("/client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(clientService).update(eq(clientId), any(ClientDto.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "AGENT"})
    void delete_ShouldDeleteClient() throws Exception {
        doNothing().when(clientService).delete(clientId);

        mockMvc.perform(delete("/client/{id}", clientId))
                .andExpect(status().isOk());

        verify(clientService).delete(clientId);
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "AGENT"})
    void getById_ShouldReturnClient() throws Exception {
        given(clientService.getById(clientId)).willReturn(clientDto);

        mockMvc.perform(get("/client/{id}", clientId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(clientService).getById(clientId);
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "AGENT"})
    void uploadDocumentAttachments_ShouldUploadAttachments() throws Exception {
        List<AttachmentDto> attachments = List.of(
            AttachmentDto.builder()
                .fileName("document.png")
                .content("base64content")
                .attachmentType(AttachmentType.IDENTIFICATION)
                .build()
        );

        doNothing().when(clientService).uploadDocumentAttachments(eq(clientId), any());

        mockMvc.perform(post("/client/attachments/{clientId}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attachments)))
                .andExpect(status().isOk());

        verify(clientService).uploadDocumentAttachments(eq(clientId), any());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "AGENT"})
    void getAll_ShouldReturnAllClients() throws Exception {
        given(clientService.getAll()).willReturn(List.of(clientDto));

        mockMvc.perform(get("/client"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(clientService).getAll();
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "AGENT"})
    void getByIdentification_ShouldReturnClient() throws Exception {
        String identificationNumber = "123456789";

        given(clientService.getByIdentification(identificationNumber)).willReturn(clientDto);

        mockMvc.perform(get("/client/identification/{id}", identificationNumber))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.identificationNumber").value(identificationNumber))
                .andExpect(jsonPath("$.name").value("John"));

        verify(clientService).getByIdentification(identificationNumber);
    }

}
