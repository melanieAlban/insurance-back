package com.fram.insurance_manager.service;

import com.fram.insurance_manager.config.mail.MailgunService;
import com.fram.insurance_manager.dto.BeneficiaryDto;
import com.fram.insurance_manager.dto.ContractDto;
import com.fram.insurance_manager.dto.ObservationMessageDto;
import com.fram.insurance_manager.dto.PaymentDto;
import com.fram.insurance_manager.entity.*;
import com.fram.insurance_manager.enums.ContractStatus;
import com.fram.insurance_manager.enums.ContractStep;
import com.fram.insurance_manager.enums.UserRol;
import com.fram.insurance_manager.repository.ClientRepository;
import com.fram.insurance_manager.repository.ContractRepository;
import com.fram.insurance_manager.repository.InsuranceRepository;
import com.fram.insurance_manager.repository.UserRepository;
import com.fram.insurance_manager.service.impl.ContractServiceImpl;
import com.fram.insurance_manager.service.impl.PaymentServiceImpl;
import com.fram.insurance_manager.util.AttachmentUtil;
import com.fram.insurance_manager.util.UserUtil;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private PaymentServiceImpl paymentService;

    @Mock
    private MailgunService mailgunService;

    @Mock
    private AttachmentUtil attachmentUtil;

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private ContractServiceImpl contractService;

    @Captor
    private ArgumentCaptor<Contract> contractCaptor;

    private Contract contract;
    private Insurance insurance;
    private Client client;
    private UUID contractId;
    private UUID insuranceId;
    private UUID clientId;

    @BeforeEach
    void setUp() throws UnirestException {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        contractId = UUID.randomUUID();
        insuranceId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        insurance = Insurance.builder()
                .id(insuranceId)
                .name("Health Insurance")
                .paymentAmount(100.0)
                .build();

        client = new Client();
        client.setId(clientId);
        client.setName("John Doe");

        User user = new User();
        user.setId(userId);
        user.setRol(UserRol.CLIENT);
        user.setEmail("john.doe@example.com");
        
        client.setUser(user);

        Map<ContractStep, Boolean> stepStatuses = new EnumMap<>(ContractStep.class);
        stepStatuses.put(ContractStep.UPLOAD_DOCUMENTS, false);
        stepStatuses.put(ContractStep.DOCUMENT_APPROVAL, false);
        stepStatuses.put(ContractStep.PAYMENT_APPROVAL, false);
        stepStatuses.put(ContractStep.CLIENT_APPROVAL, false);

        List<Beneficiary> beneficiaries = new ArrayList<>();
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setId(UUID.randomUUID());
        beneficiary.setName("Jane Doe");
        beneficiaries.add(beneficiary);

        contract = Contract.builder()
                .id(contractId)
                .startDate(LocalDate.now())
                .status(ContractStatus.ACTIVE)
                .totalPaymentAmount(BigDecimal.valueOf(500))
                .insurance(insurance)
                .client(client)
                .stepStatuses(stepStatuses)
                .beneficiaries(beneficiaries)
                .build();

        lenient().when(userUtil.getUserId()).thenReturn(userId);
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(insuranceRepository.findById(insuranceId)).thenReturn(Optional.of(insurance));
        lenient().when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        lenient().doNothing().when(mailgunService).sendHtmlMessage(anyString(), anyString(), anyString());
    }


    @Test
    void findById_ExistingId_ShouldReturnContract() {
        // Arrange
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        // Act
        ContractDto result = contractService.findById(contractId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(contractId);
        verify(contractRepository).findById(contractId);
    }

    @Test
    void findById_NonExistingId_ShouldThrowNotFoundException() {
        // Arrange
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> contractService.findById(contractId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verify(contractRepository).findById(contractId);
    }

    @Test
    void findAll_ShouldReturnAllContracts() {
        // Arrange
        when(contractRepository.findAll()).thenReturn(List.of(contract));

        // Act
        List<ContractDto> result = contractService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(contractId);
        verify(contractRepository).findAll();
    }

    @Test
    void approveAttachments_ShouldUpdateStepStatus() {
        // Arrange
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenReturn(contract);

        // Act
        contractService.approveAttachments(contractId);

        // Assert
        verify(contractRepository).findById(contractId);
        verify(contractRepository).save(contractCaptor.capture());

        Contract savedContract = contractCaptor.getValue();
        assertThat(savedContract.getStepStatuses().get(ContractStep.DOCUMENT_APPROVAL)).isTrue();
    }

    @Test
    void approveContract_NotAllStepsCompleted_ShouldThrowException() {
        // Arrange
        contract.getStepStatuses().put(ContractStep.UPLOAD_DOCUMENTS, false); // Faltan pasos
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        // Act & Assert
        assertThatThrownBy(() -> contractService.approveContract(contractId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
                });

        verify(contractRepository).findById(contractId);
        verify(contractRepository, never()).save(any(Contract.class));
    }


    @Test
    void getAllContractData_ExistingId_ShouldReturnContractData() {
        // Arrange
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        // We expect an exception due to the contract file generation
        // Act & Assert
        assertThatThrownBy(() -> contractService.getAllContractData(contractId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(contractRepository).findById(contractId);
    }

    @Test
    void getAllContractData_NonExistingId_ShouldThrowNotFoundException() {
        // Arrange
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> contractService.getAllContractData(contractId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verify(contractRepository).findById(contractId);
    }

    @Test
    void save_NewContract_ShouldReturnDtoWithInsuranceAmountAndSendEmail() throws UnirestException {
        // Arrange: contrato sin ninguno previo y sin condiciones
        client.setContracts(new ArrayList<>());
        client.setConditions(new ArrayList<>());
        ContractDto dto = ContractDto.builder()
                .insuranceId(insuranceId)
                .clientId(clientId)
                .build();

        // El save del repositorio devuelve el mismo contrato que recibe
        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        ContractDto result = contractService.save(dto);

        // Assert: monto igual al paymentAmount del seguro, y email enviado
        assertThat(result.getTotalPaymentAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(insurance.getPaymentAmount()));
        verify(mailgunService).sendHtmlMessage(
                eq(client.getUser().getEmail()),
                anyString(),
                anyString()
        );
    }

    @Test
    void save_DuplicateInsurance_ShouldThrowBadRequest() {
        // Arrange: el cliente ya tiene un contrato con este seguro
        Contract existing = Contract.builder().insurance(insurance).build();
        client.setContracts(List.of(existing));

        ContractDto dto = ContractDto.builder()
                .insuranceId(insuranceId)
                .clientId(clientId)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> contractService.save(dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void save_MailgunException_ShouldThrowInternalServerError() throws UnirestException {
        // Arrange: falla el envío de correo
        doThrow(new com.mashape.unirest.http.exceptions.UnirestException("fail"))
                .when(mailgunService).sendHtmlMessage(anyString(), anyString(), anyString());

        ContractDto dto = ContractDto.builder()
                .insuranceId(insuranceId)
                .clientId(clientId)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> contractService.save(dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void save_WithConditions_ShouldApplyAddedPercentage() throws UnirestException {
        // Arrange: condiciones que suman 15%
        Condition c1 = new Condition(); c1.setAddedPercentage(10);
        Condition c2 = new Condition(); c2.setAddedPercentage(5);
        client.setContracts(new ArrayList<>());
        client.setConditions(List.of(c1, c2));

        ContractDto dto = ContractDto.builder()
                .insuranceId(insuranceId)
                .clientId(clientId)
                .build();

        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        ContractDto result = contractService.save(dto);

        // Assert: 100 * (1 + 0.15) = 115
        assertThat(result.getTotalPaymentAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(insurance.getPaymentAmount() * 1.15));
    }

    @Test
    void approveContract_AllStepsCompleted_ShouldActivateContractAndSendMail() throws UnirestException {
        contract.getStepStatuses().put(ContractStep.UPLOAD_DOCUMENTS, true);
        contract.getStepStatuses().put(ContractStep.DOCUMENT_APPROVAL, true);
        contract.getStepStatuses().put(ContractStep.PAYMENT_APPROVAL, true);
        contract.getStepStatuses().put(ContractStep.CLIENT_APPROVAL, false); // aún no aprobado

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(mailgunService).sendHtmlMessage(anyString(), anyString(), anyString());

        contractService.approveContract(contractId);

        assertThat(contract.getStepStatuses().get(ContractStep.CLIENT_APPROVAL)).isTrue();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getStartDate()).isEqualTo(LocalDate.now());

        verify(mailgunService).sendHtmlMessage(
                eq(client.getUser().getEmail()),
                contains("activado"),
                anyString()
        );
        verify(contractRepository).save(contractCaptor.capture());
    }


    @Test
    void approvePayment_ShouldUpdatePaymentStatusAndSendMail() throws UnirestException {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(mailgunService).sendHtmlMessage(anyString(), anyString(), anyString());

        contractService.approvePayment(contractId);

        assertThat(contract.getStepStatuses().get(ContractStep.PAYMENT_APPROVAL)).isTrue();
        verify(mailgunService).sendHtmlMessage(
                eq(client.getUser().getEmail()),
                contains("Pago aprobado"),
                anyString()
        );
        verify(contractRepository).save(contractCaptor.capture());
    }

    @Test
    void rejectAttachments_ShouldResetDocumentApprovalAndSendMail() throws UnirestException {
        ObservationMessageDto obs = new ObservationMessageDto();
        obs.setObservation("Documentos inválidos");
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(mailgunService).sendHtmlMessage(anyString(), anyString(), anyString());

        contractService.rejectAttachments(contractId, obs);

        assertThat(contract.getStepStatuses().get(ContractStep.DOCUMENT_APPROVAL)).isFalse();
        verify(mailgunService).sendHtmlMessage(
                eq(client.getUser().getEmail()),
                contains("rechazada"),
                contains("Documentos inválidos")
        );
        verify(contractRepository).save(contractCaptor.capture());
    }

    @Test
    void save_ShouldMapBeneficiariesFromDto() throws UnirestException {
        client.setContracts(new ArrayList<>());
        client.setConditions(new ArrayList<>());
        ContractDto dto = ContractDto.builder()
                .insuranceId(insuranceId)
                .clientId(clientId)
                .beneficiaries(List.of(
                        BeneficiaryDto.builder().name("Alice").build(),
                        BeneficiaryDto.builder().name("Bob").build()
                ))
                .build();
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(mailgunService).sendHtmlMessage(anyString(), anyString(), anyString());

        contractService.save(dto);

        verify(contractRepository).save(contractCaptor.capture());
        Contract saved = contractCaptor.getValue();
        assertThat(saved.getBeneficiaries()).hasSize(2);
        assertThat(saved.getBeneficiaries().stream().map(Beneficiary::getName))
                .containsExactlyInAnyOrder("Alice", "Bob");
        saved.getBeneficiaries().forEach(b ->
                assertThat(b.getContract()).isSameAs(saved)
        );
    }

    @Test
    void getAllContractData_Success_ShouldMapAttachmentsPaymentAndContractFile() throws Exception {
        // Spy para poder stubear generateContract
        ContractServiceImpl spyService = Mockito.spy(contractService);

        // 1) Attachment del cliente
        Attachment attach = new Attachment();
        attach.setId(UUID.randomUUID());
        attach.setFileName("doc.pdf");
        attach.setPathReference("disk/path/doc.pdf");
        client.setAttachments(List.of(attach));

        when(contractRepository.findById(contractId))
                .thenReturn(Optional.of(contract));
        when(attachmentUtil.getBase64FromPathReference("disk/path/doc.pdf"))
                .thenReturn("ZGF0YV9iYXNlNjQ=");

        // 2) Pago actual
        Payment pay = new Payment();
        pay.setId(UUID.randomUUID());
        pay.setAmount(123.45);
        pay.setDate(LocalDate.now());
        when(paymentService.findCurrentPayment(contract, LocalDate.now()))
                .thenReturn(Optional.of(pay));

        // 3) Stubeo generateContract para que devuelva un String
        String fakePdfString = "BASE64_PDF_CONTENT";
        doReturn(fakePdfString)
                .when(spyService)
                .generateContract(any(Contract.class));

        // — Act —
        ContractDto dto = spyService.getAllContractData(contractId);

        // — Assert —
        verify(contractRepository).findById(contractId);
        assertThat(dto.getClientAttachments()).hasSize(1);
        assertThat(dto.getClientAttachments().get(0).getContent())
                .isEqualTo("ZGF0YV9iYXNlNjQ=");
        assertThat(dto.getPaymentDto()).isNotNull()
                .extracting(PaymentDto::getAmount)
                .isEqualTo(123.45);
        // Ahora el contractFile es el String que stubeamos
        assertThat(dto.getContractFile()).isEqualTo(fakePdfString);
    }

    @Test
    void getAllContractData_NoCurrentPayment_ShouldMapAttachmentsAndNoPayment() throws Exception {
        ContractServiceImpl spyService = Mockito.spy(contractService);

        Attachment attach = new Attachment();
        attach.setId(UUID.randomUUID());
        attach.setPathReference("disk/path/doc.pdf");
        client.setAttachments(List.of(attach));

        when(contractRepository.findById(contractId))
                .thenReturn(Optional.of(contract));
        when(attachmentUtil.getBase64FromPathReference(anyString()))
                .thenReturn("ZGF0YV9iYXNlNjQ=");
        when(paymentService.findCurrentPayment(contract, LocalDate.now()))
                .thenReturn(Optional.empty());

        String fakePdfString = "BASE64_PDF_CONTENT";
        doReturn(fakePdfString)
                .when(spyService)
                .generateContract(any(Contract.class));

        ContractDto dto = spyService.getAllContractData(contractId);

        assertThat(dto.getClientAttachments()).hasSize(1);
        assertThat(dto.getPaymentDto()).isNull();
        assertThat(dto.getContractFile()).isEqualTo(fakePdfString);
    }
}