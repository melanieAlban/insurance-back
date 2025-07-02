package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.ClientDto;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.entity.Attachment;
import com.fram.insurance_manager.entity.Client;
import com.fram.insurance_manager.entity.Contract;
import com.fram.insurance_manager.entity.User;
import com.fram.insurance_manager.enums.AttachmentType;
import com.fram.insurance_manager.enums.ContractStep;
import com.fram.insurance_manager.enums.UserRol;
import com.fram.insurance_manager.repository.ClientRepository;
import com.fram.insurance_manager.repository.UserRepository;
import com.fram.insurance_manager.service.impl.ClientServiceImpl;
import com.fram.insurance_manager.util.AttachmentUtil;
import com.fram.insurance_manager.util.UserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private AttachmentUtil attachmentUtil;

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserUtil userUtil;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client client;
    private UUID clientId;
    private List<AttachmentDto> attachments;
    private Path mockPath;
    private ClientDto clientDto;
    private User user;
    private UserDto userDto;
    private SaveUserDto saveUserDto;
    private List<Client> clientList;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();

        // Set up User
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("john.doe@example.com");
        user.setPassword("password");
        user.setRol(UserRol.CLIENT);

        // Set up Client
        client = new Client();
        client.setId(clientId);
        client.setName("John");
        client.setLastName("Doe");
        client.setIdentificationNumber("123456789");
        client.setAttachments(new ArrayList<>());
        client.setContracts(new ArrayList<>());
        client.setUser(user);

        // Set up ClientDto
        clientDto = new ClientDto();
        clientDto.setId(clientId);
        clientDto.setName("John");
        clientDto.setLastName("Doe");
        clientDto.setIdentificationNumber("123456789");

        // Set up UserDto
        userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail("john.doe@example.com");
        userDto.setRol(UserRol.CLIENT);

        // Set up SaveUserDto
        saveUserDto = new SaveUserDto();
        saveUserDto.setEmail("john.doe@example.com");
        saveUserDto.setRol(UserRol.CLIENT);

        clientDto.setUser(saveUserDto);

        // Set up client list for getAll test
        clientList = new ArrayList<>();
        clientList.add(client);

        // Set up attachments for document upload tests
        attachments = List.of(
                AttachmentDto.builder()
                        .fileName("id.png")
                        .content("base64content")
                        .attachmentType(AttachmentType.IDENTIFICATION)
                        .build(),
                AttachmentDto.builder()
                        .fileName("photo.png")
                        .content("base64content")
                        .attachmentType(AttachmentType.PORTRAIT_PHOTO)
                        .build()
        );

        mockPath = Paths.get("C:\\data\\12345_Doe.png");
    }

    @Test
    void getAll_returnsAllClients() {
        // Arrange
        when(clientRepository.findAll()).thenReturn(clientList);
        when(modelMapper.map(any(Client.class), eq(ClientDto.class))).thenReturn(clientDto);

        // Act
        List<ClientDto> result = clientService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0)).isEqualTo(clientDto);
        verify(clientRepository).findAll();
    }

    @Test
    void getById_existingClient_returnsClientDto() {
        // Arrange
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(modelMapper.map(client, ClientDto.class)).thenReturn(clientDto);

        // Act
        ClientDto result = clientService.getById(clientId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(clientDto);
        verify(clientRepository).findById(clientId);
    }

    @Test
    void getById_nonExistingClient_throwsResponseStatusException() {
        // Arrange
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> clientService.getById(clientId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("Cliente no encontrado");
    }

    @Test
    void getByIdentification_existingClient_returnsClientDto() {
        // Arrange
        String identificationNumber = "123456789";
        when(clientRepository.findByIdentificationNumber(identificationNumber)).thenReturn(client);
        when(modelMapper.map(client, ClientDto.class)).thenReturn(clientDto);

        // Act
        ClientDto result = clientService.getByIdentification(identificationNumber);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(clientDto);
        verify(clientRepository).findByIdentificationNumber(identificationNumber);
    }

    @Test
    void getByIdentification_nonExistingClient_throwsResponseStatusException() {
        // Arrange
        String identificationNumber = "nonexistent";
        when(clientRepository.findByIdentificationNumber(identificationNumber)).thenReturn(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> clientService.getByIdentification(identificationNumber));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("Cliente no encontrado");
    }

    @Test
    void uploadDocumentAttachments_clientNotFound_throws404() {
        // Arrange
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () ->
                        clientService.uploadDocumentAttachments(clientId, attachments),
                "Should throw ResponseStatusException when client not found"
        );
    }

    @Test
    void uploadDocumentAttachments_invalidNumberOfAttachments_throwsBadRequest() {
        // Arrange
        List<AttachmentDto> singleAttachment = List.of(
                AttachmentDto.builder()
                        .fileName("id.png")
                        .content("base64content")
                        .attachmentType(AttachmentType.IDENTIFICATION)
                        .build()
        );

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                clientService.uploadDocumentAttachments(clientId, singleAttachment)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Son dos archivos");
    }

    @Test
    void uploadDocumentAttachments_missingRequiredTypes_throwsBadRequest() {
        // Arrange
        List<AttachmentDto> wrongTypeAttachments = List.of(
                AttachmentDto.builder()
                        .fileName("id.png")
                        .content("base64content")
                        .attachmentType(AttachmentType.IDENTIFICATION)
                        .build(),
                AttachmentDto.builder()
                        .fileName("payment.png")
                        .content("base64content")
                        .attachmentType(AttachmentType.PAYMENT_PROOF)
                        .build()
        );

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                clientService.uploadDocumentAttachments(clientId, wrongTypeAttachments)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Los archivos deben incluir uno de identificación y otro de tipo retrato");
    }

    @Test
    void uploadDocumentAttachments_withExistingAttachments_updatesAttachments() {
        // Arrange
        Attachment existingAttachment = new Attachment();
        existingAttachment.setFileName("old-id.png");
        existingAttachment.setAttachmentType(AttachmentType.IDENTIFICATION);
        existingAttachment.setPathReference("C:\\data\\old_Doe.png");
        client.getAttachments().add(existingAttachment);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(modelMapper.map(any(AttachmentDto.class), eq(Attachment.class))).thenAnswer(invocation -> {
            AttachmentDto dto = invocation.getArgument(0);
            Attachment attachment = new Attachment();
            attachment.setFileName(dto.getFileName());
            attachment.setAttachmentType(dto.getAttachmentType());
            return attachment;
        });

        // Use specific mocking for each attachment type
        when(attachmentUtil.saveAttachmentOnDiskBase64(
                eq("base64content"), 
                eq("id.png"), 
                eq("Doe"), 
                eq("C:\\data\\old_Doe.png"))).thenReturn(mockPath);

        when(attachmentUtil.saveAttachmentOnDiskBase64(
                eq("base64content"), 
                eq("photo.png"), 
                eq("Doe"), 
                isNull())).thenReturn(mockPath);

        // Act
        clientService.uploadDocumentAttachments(clientId, attachments);

        // Assert
        verify(clientRepository).save(client);

        // Verify the existing attachment was updated
        assertThat(existingAttachment.getFileName()).isEqualTo("id.png");
    }

    @Test
    void save_validClientDto_returnsClientDto() {
        // Arrange
        String randomPassword = "randomPassword";
        when(userUtil.generateRandomPassword()).thenReturn(randomPassword);
        when(authService.register(any(SaveUserDto.class))).thenReturn(userDto);

        // Use lenient mocking for modelMapper to handle different argument types
        lenient().when(modelMapper.map(any(), eq(Client.class))).thenReturn(client);
        lenient().when(modelMapper.map(any(), eq(User.class))).thenReturn(user);
        lenient().when(modelMapper.map(any(Client.class), eq(ClientDto.class))).thenReturn(clientDto);

        when(clientRepository.save(any(Client.class))).thenReturn(client);

        // Act
        ClientDto result = clientService.save(clientDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(clientDto);
        verify(userUtil).generateRandomPassword();
        verify(authService).register(any(SaveUserDto.class));
        verify(clientRepository).save(any(Client.class));
        verify(userUtil).sendCredentialsToUser(anyString(), anyString(), anyString());
    }

    @Test
    void update_validClientDto_returnsUpdatedClientDto() {
        // Arrange
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        doAnswer(invocation -> {
            client.setName(clientDto.getName());
            client.setLastName(clientDto.getLastName());
            return null;
        }).when(modelMapper).map(eq(clientDto), eq(client));
        when(modelMapper.map(client, ClientDto.class)).thenReturn(clientDto);

        // Act
        ClientDto result = clientService.update(clientId, clientDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(clientDto);
        verify(clientRepository).findById(clientId);
        verify(clientRepository).save(client);
    }

    @Test
    void update_emailChanged_throwsPreconditionFailed() {
        // Arrange
        SaveUserDto changedEmailUserDto = new SaveUserDto();
        changedEmailUserDto.setEmail("changed@example.com");
        changedEmailUserDto.setRol(UserRol.CLIENT);

        ClientDto changedEmailClientDto = new ClientDto();
        changedEmailClientDto.setId(clientId);
        changedEmailClientDto.setName("John");
        changedEmailClientDto.setLastName("Doe");
        changedEmailClientDto.setUser(changedEmailUserDto);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> clientService.update(clientId, changedEmailClientDto));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(exception.getReason()).contains("El email no puede ser modificado");
    }

    @Test
    void delete_existingClient_deletesClientAndUser() {
        // Arrange
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act
        clientService.delete(clientId);

        // Assert
        verify(clientRepository).findById(clientId);
        verify(userRepository).deleteById(user.getId());
        verify(clientRepository).deleteById(clientId);
    }

    @Test
    void delete_nonExistingClient_throwsResponseStatusException() {
        // Arrange
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> clientService.delete(clientId));

        verify(clientRepository).findById(clientId);
        verify(userRepository, never()).deleteById(any());
        verify(clientRepository, never()).deleteById(any());
    }

    @Test
    void uploadDocumentAttachments_withContracts_updatesContractSteps() {
        // Arrange
        Contract contract = new Contract();
        contract.setStepStatuses(new HashMap<>());
        client.getContracts().add(contract);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(modelMapper.map(any(AttachmentDto.class), eq(Attachment.class))).thenAnswer(invocation -> {
            AttachmentDto dto = invocation.getArgument(0);
            Attachment attachment = new Attachment();
            attachment.setFileName(dto.getFileName());
            attachment.setAttachmentType(dto.getAttachmentType());
            return attachment;
        });

        // Use specific mocking for each attachment type
        when(attachmentUtil.saveAttachmentOnDiskBase64(
                eq("base64content"), 
                eq("id.png"), 
                eq("Doe"), 
                isNull())).thenReturn(mockPath);

        when(attachmentUtil.saveAttachmentOnDiskBase64(
                eq("base64content"), 
                eq("photo.png"), 
                eq("Doe"), 
                isNull())).thenReturn(mockPath);

        // Act
        clientService.uploadDocumentAttachments(clientId, attachments);

        // Assert
        verify(clientRepository).save(client);
        verify(attachmentUtil).validateAttachment(attachments.get(0));
        verify(attachmentUtil).validateAttachment(attachments.get(1));
        assertThat(contract.getStepStatuses()).containsEntry(ContractStep.UPLOAD_DOCUMENTS, true);
    }

    @Test
    void uploadDocumentAttachments_validatesEachAttachment() {
        // Arrange
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(modelMapper.map(any(AttachmentDto.class), eq(Attachment.class))).thenAnswer(invocation -> {
            AttachmentDto dto = invocation.getArgument(0);
            Attachment attachment = new Attachment();
            attachment.setFileName(dto.getFileName());
            attachment.setAttachmentType(dto.getAttachmentType());
            return attachment;
        });

        // Use specific mocking for each attachment type
        when(attachmentUtil.saveAttachmentOnDiskBase64(
                eq("base64content"), 
                eq("id.png"), 
                eq("Doe"), 
                isNull())).thenReturn(mockPath);

        when(attachmentUtil.saveAttachmentOnDiskBase64(
                eq("base64content"), 
                eq("photo.png"), 
                eq("Doe"), 
                isNull())).thenReturn(mockPath);

        // Act
        clientService.uploadDocumentAttachments(clientId, attachments);

        // Assert
        // Verify that validateAttachment is called for each attachment
        verify(attachmentUtil).validateAttachment(attachments.get(0));
        verify(attachmentUtil).validateAttachment(attachments.get(1));
    }
}
