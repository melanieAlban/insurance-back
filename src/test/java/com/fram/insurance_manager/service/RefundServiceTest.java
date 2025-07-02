package com.fram.insurance_manager.service;

import com.fram.insurance_manager.config.mail.MailgunService;
import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.RefundRequestDto;
import com.fram.insurance_manager.entity.*;
import com.fram.insurance_manager.enums.AttachmentType;
import com.fram.insurance_manager.enums.ContractStatus;
import com.fram.insurance_manager.enums.RefundRequestStatus;
import com.fram.insurance_manager.enums.UserRol;
import com.fram.insurance_manager.repository.AttachmentRepository;
import com.fram.insurance_manager.repository.ContractRepository;
import com.fram.insurance_manager.repository.RefundRequestRepository;
import com.fram.insurance_manager.repository.UserRepository;
import com.fram.insurance_manager.service.impl.RefundRequestServiceImpl;
import com.fram.insurance_manager.util.AttachmentUtil;
import com.fram.insurance_manager.util.UserUtil;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefundServiceTest {

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private MailgunService mailgunService;

    @Mock
    private AttachmentUtil attachmentUtil;

    @Mock
    private UserUtil userUtil;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ModelMapper modelMapper;

    @Captor
    private ArgumentCaptor<RefundRequest> refundRequestArgumentCaptor;

    @InjectMocks
    private RefundRequestServiceImpl refundRequestService;

    private RefundRequest getRefundRequest() {
        return RefundRequest.builder()
                .id(UUID.randomUUID())
                .status(RefundRequestStatus.APPROVED)
                .description("Descripcion de prueba")
                .amountPaid(50.0)
                .coveredAmount(50.0)
                .contract(Contract.builder().id(UUID.randomUUID()).client(Client.builder().user(User.builder().email("testemail").build()).build()).build())
                .attachments(new ArrayList<>(List.of(
                        Attachment.builder()
                                .id(UUID.randomUUID())
                                .attachmentType(AttachmentType.REFUND_REQUEST)
                                .fileName("refund_request.pdf")
                                .pathReference("path/to/refund_request.pdf")
                                .build()
                )))
                .build();
    }

    private RefundRequestDto getRefundRequestDto() {
        return RefundRequestDto.builder()
                .id(UUID.randomUUID())
                .status(RefundRequestStatus.APPROVED)
                .description("Descripcion de prueba")
                .coveredAmount(50.0)
                .contractId(UUID.randomUUID())
                .attachments(List.of(AttachmentDto.builder()
                        .id(UUID.randomUUID())
                        .attachmentType(AttachmentType.REFUND_REQUEST)
                        .fileName("refund_request.pdf")
                        .content("base64content")
                        .build()))
                .build();
    }

    @Test
    void shouldGetRefundRequestsWhenUserIsClient() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Client client = new Client();
        client.setId(clientId);

        User user = new User();
        user.setId(userId);
        user.setRol(UserRol.CLIENT);
        user.setClient(client);

        RefundRequest refundRequest = getRefundRequest();
        refundRequest.getContract().setClient(client);

        when(userUtil.getUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refundRequestRepository.findAllByContract_Client_Id(clientId)).thenReturn(List.of(refundRequest));

        // Act
        List<RefundRequestDto> result = refundRequestService.getRefundRequests();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(refundRequest.getId());
        assertThat(result.get(0).getContractId()).isEqualTo(refundRequest.getContract().getId());
    }

    @Test
    void shouldGetRefundRequests_whenUserIsAdmin() {
        // Arrange
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setRol(UserRol.ADMIN);
        user.setClient(null);

        RefundRequest refundRequest = getRefundRequest();

        when(userUtil.getUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refundRequestRepository.findAll()).thenReturn(List.of(refundRequest));

        // Act
        List<RefundRequestDto> result = refundRequestService.getRefundRequests();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(refundRequest.getId());
        assertThat(result.get(0).getContractId()).isEqualTo(refundRequest.getContract().getId());
    }

    @Test
    void shouldFailGetRefundRequestsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(userUtil.getUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundRequestService.getRefundRequests())
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldGetRefundRequest() {
        RefundRequest refundRequest = getRefundRequest();
        when(refundRequestRepository.findById(any())).thenReturn(Optional.of(refundRequest));

        RefundRequestDto result = refundRequestService.getRefundRequest(refundRequest.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(refundRequest.getId());
        assertThat(result.getContractId()).isEqualTo(refundRequest.getContract().getId());
    }

    @Test
    void shouldGetRefundRequestByNotFound() {
        when(refundRequestRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundRequestService.getRefundRequest(UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldSaveRefundRequest() throws UnirestException {
        // Arrange
        RefundRequest refundRequest = getRefundRequest();
        RefundRequestDto refundRequestDto = getRefundRequestDto();
        refundRequestDto.setPaidAmount(50.00);

        Contract contract = Contract.builder()
                .id(refundRequestDto.getContractId())
                .active(true)
                .client(Client.builder().user(User.builder().email("testemail").build()).build())
                .insurance(Insurance.builder().coverage(100.0).build())
                .payments(List.of())
                .status(ContractStatus.ACTIVE)
                .build();

        Path dummyPath = Path.of("path/to/refund_request.pdf");

        when(contractRepository.findById(any())).thenReturn(Optional.of(contract));
        when(refundRequestRepository.save(any())).thenReturn(refundRequest);
        when(attachmentUtil.saveAttachmentOnDiskBase64(any(), any(), any(), any())).thenReturn(dummyPath);

        doNothing().when(mailgunService).sendHtmlMessage(any(), any(), any());

        // Act
        RefundRequestDto result = refundRequestService.save(refundRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals(refundRequest.getId(), result.getId());
        verify(attachmentUtil, times(refundRequestDto.getAttachments().size()))
                .saveAttachmentOnDiskBase64(any(), any(), any(), any());
        verify(refundRequestRepository).save(any());
        verify(mailgunService, times(1)).sendHtmlMessage(any(), any(), any());
    }

    @Test
    void shouldFailSaveByContractNotFound() {
        when(contractRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundRequestService.save(RefundRequestDto.builder().contractId(UUID.randomUUID()).build()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldFailSaveByContractNotActive() {
        when(contractRepository.findById(any())).thenReturn(Optional.of(Contract.builder().status(ContractStatus.CANCELLED).build()));

        assertThatThrownBy(() -> refundRequestService.save(RefundRequestDto.builder().contractId(UUID.randomUUID()).build()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.PRECONDITION_FAILED);
    }

    @Test
    void shouldReject() throws UnirestException {
        UUID id = UUID.randomUUID();
        String reason = "";

        RefundRequest refundRequest = getRefundRequest();
        refundRequest.setId(id);
        refundRequest.setStatus(RefundRequestStatus.NEW);
        refundRequest.setObservation("");

        when(refundRequestRepository.findById(eq(id))).thenReturn(Optional.of(refundRequest));
        doNothing().when(mailgunService).sendHtmlMessage(any(), any(), any());

        refundRequestService.reject(id, reason);

        verify(mailgunService, times(1)).sendHtmlMessage(any(), any(), any());
        verify(refundRequestRepository, times(1)).save(refundRequestArgumentCaptor.capture());

        RefundRequest saved = refundRequestArgumentCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(RefundRequestStatus.REJECTED);
        assertThat(saved.getObservation()).isEqualTo("Razón de rechazo: ");
    }

    @Test
    void shouldApproveRefundRequest() throws UnirestException {
        // Arrange
        UUID id = UUID.randomUUID();

        RefundRequest refundRequest = getRefundRequest();
        refundRequest.setId(id);
        refundRequest.setStatus(RefundRequestStatus.NEW); // estado inicial
        refundRequest.setCoveredAmount(80.0);

        // mocks necesarios
        when(refundRequestRepository.findById(eq(id))).thenReturn(Optional.of(refundRequest));
        doNothing().when(mailgunService).sendHtmlMessage(any(), any(), any());

        // Act
        refundRequestService.approve(id);

        // Assert
        verify(mailgunService, times(1)).sendHtmlMessage(
                eq(refundRequest.getContract().getClient().getUser().getEmail()),
                any(), // subject
                any()  // body
        );

        verify(refundRequestRepository, times(1)).save(refundRequestArgumentCaptor.capture());

        RefundRequest saved = refundRequestArgumentCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(RefundRequestStatus.APPROVED);
    }

    @Test
    void shouldUpdateAttachmentsWithOneNewAttachment() {
        // Arrange
        RefundRequest refundRequest = getRefundRequest();
        refundRequest.setContract(Contract.builder().id(UUID.randomUUID()).client(Client.builder().lastName("lastname").user(User.builder().email("testemail").build()).build()).build());
        UUID refundRequestId = refundRequest.getId();
        AttachmentDto dto = AttachmentDto.builder()
                .id(null)
                .fileName("document.pdf")
                .content("base64content")
                .attachmentType(AttachmentType.REFUND_REQUEST)
                .build();

        Path fakePath = Path.of("some/path/document.pdf");

        when(refundRequestRepository.findById(refundRequestId)).thenReturn(Optional.of(refundRequest));
        doNothing().when(attachmentUtil).validateAttachment(dto);
        when(attachmentRepository.findById(null)).thenReturn(Optional.empty());
        when(attachmentUtil.saveAttachmentOnDiskBase64(any(), any(), any(), any())).thenReturn(fakePath);

        // Act
        refundRequestService.updateAttachments(refundRequestId, List.of(dto));

        // Assert
        verify(attachmentUtil).validateAttachment(dto);
        verify(attachmentUtil).saveAttachmentOnDiskBase64(any(), any(), any(), any());
        verify(refundRequestRepository).save(any());
    }

    @Test
    void shouldFailUpdateAttachments_whenListNullOrEmptyOrTooBig() {
        UUID id = UUID.randomUUID();
        when(refundRequestRepository.findById(id)).thenReturn(Optional.of(getRefundRequest()));

        assertThatThrownBy(() -> refundRequestService.updateAttachments(id, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.PRECONDITION_FAILED);

        assertThatThrownBy(() -> refundRequestService.updateAttachments(id, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.PRECONDITION_FAILED);

        List<AttachmentDto> four = List.of(new AttachmentDto(), new AttachmentDto(), new AttachmentDto(), new AttachmentDto());
        assertThatThrownBy(() -> refundRequestService.updateAttachments(id, four))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.PRECONDITION_FAILED);
    }

    @Test
    void shouldRemoveMissingAndAddNewAttachments() {
        RefundRequest refundRequest = getRefundRequest();
        refundRequest.setContract(Contract.builder().id(UUID.randomUUID()).client(Client.builder().lastName("lastname").user(User.builder().email("testemail").build()).build()).build());

        refundRequest.setAttachments(new ArrayList<>(refundRequest.getAttachments()));
        UUID id = refundRequest.getId();

        AttachmentDto existingDto = AttachmentDto.builder()
                .id(refundRequest.getAttachments().get(0).getId())
                .content("ignored")
                .fileName("ignored")
                .attachmentType(AttachmentType.REFUND_REQUEST)
                .build();

        AttachmentDto newDto = AttachmentDto.builder()
                .id(null)
                .content("base64")
                .fileName("new.pdf")
                .attachmentType(AttachmentType.REFUND_REQUEST)
                .build();

        when(refundRequestRepository.findById(id)).thenReturn(Optional.of(refundRequest));
        doNothing().when(attachmentUtil).validateAttachment(any());
        when(attachmentRepository.findById(existingDto.getId())).thenReturn(Optional.of(refundRequest.getAttachments().get(0)));
        when(attachmentRepository.findById(null)).thenReturn(Optional.empty());
        when(attachmentUtil.saveAttachmentOnDiskBase64(any(), any(), any(), any()))
                .thenReturn(Path.of("some/path/new.pdf"));

        refundRequestService.updateAttachments(id, List.of(existingDto, newDto));

        verify(attachmentUtil).validateAttachment(existingDto);
        verify(attachmentUtil).validateAttachment(newDto);
        verify(refundRequestRepository).save(refundRequestArgumentCaptor.capture());

        RefundRequest saved = refundRequestArgumentCaptor.getValue();
        assertThat(saved.getAttachments()).hasSize(2);
        assertThat(saved.getAttachments()
                .stream()
                .map(Attachment::getFileName))
                .containsExactlyInAnyOrder("refund_request.pdf", "new.pdf");
    }

    @Test
    void shouldNotThrow_whenAllDtosExist() {
        RefundRequest r = getRefundRequest();
        r.setAttachments(new ArrayList<>(r.getAttachments()));
        UUID id = r.getId();

        AttachmentDto dto = AttachmentDto.builder()
                .id(r.getAttachments().get(0).getId())
                .content("ignored")
                .fileName("ignored")
                .attachmentType(AttachmentType.REFUND_REQUEST)
                .build();

        when(refundRequestRepository.findById(id)).thenReturn(Optional.of(r));
        doNothing().when(attachmentUtil).validateAttachment(dto);
        when(attachmentRepository.findById(dto.getId())).thenReturn(Optional.of(r.getAttachments().get(0)));

        // debe pasar sin excepción y no volver a salvar un nuevo archivo
        refundRequestService.updateAttachments(id, List.of(dto));

        verify(attachmentUtil).validateAttachment(dto);
        verify(attachmentUtil, never()).saveAttachmentOnDiskBase64(any(), any(), any(), any());
        verify(refundRequestRepository).save(any());
    }

}
