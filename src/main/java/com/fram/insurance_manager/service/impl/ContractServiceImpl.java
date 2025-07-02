package com.fram.insurance_manager.service.impl;

import com.fram.insurance_manager.config.mail.MailgunService;
import com.fram.insurance_manager.dto.*;
import com.fram.insurance_manager.entity.*;
import com.fram.insurance_manager.enums.ContractStatus;
import com.fram.insurance_manager.enums.ContractStep;
import com.fram.insurance_manager.enums.UserRol;
import com.fram.insurance_manager.repository.*;
import com.fram.insurance_manager.service.ContractService;
import com.fram.insurance_manager.util.AttachmentUtil;
import com.fram.insurance_manager.util.UserUtil;
import com.mashape.unirest.http.exceptions.UnirestException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {
    private final ContractRepository contractRepo;
    private final InsuranceRepository insuranceRepo;
    private final ClientRepository clientRepo;
    private final AttachmentUtil attachmentUtil;
    private final ModelMapper mapper;
    private final UserUtil userUtil;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final MailgunService mailgunService;
    private final SpringTemplateEngine templateEngine;
    private final PaymentServiceImpl paymentService;

    @Override
    public ContractDto save(ContractDto dto) {
        Contract contract = contractDtoToEntity(dto);
        contract.setStepStatuses(initialStepStatuses(contract));

        checkClientInsurance(contract.getClient(), contract.getInsurance().getId());

        addConditionAmount(contract);

        if (!contract.getStepStatuses().getOrDefault(ContractStep.UPLOAD_DOCUMENTS, false)) {
            sendMailForUploadDocumentStatus(contract.getClient().getUser().getEmail());
        }

        return contractEntityToDto(contractRepo.save(contract));
    }

    @Override
    public ContractDto findById(UUID id) {
        return mapper.map(contractRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), ContractDto.class);
    }

    @Override
    public List<ContractDto> findAll() {
        List<Contract> contracts;

        User user = this.userRepository.findById(userUtil.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Client client = user.getClient();
        UserRol rol = user.getRol();

        if (rol.equals(UserRol.CLIENT) && client != null) {
            contracts = contractRepo.findAllByClientId(client.getId());
        } else {
            contracts = contractRepo.findAll();
        }

        return contracts.stream().map(contract -> {
            ContractDto contractDto = contractEntityToDto(contract);

            boolean allStepsCompleted = contract.getStepStatuses().entrySet().stream()
                    .filter(entry -> entry.getKey() != ContractStep.CLIENT_APPROVAL)
                    .allMatch(entry -> Boolean.TRUE.equals(entry.getValue()));

            if (allStepsCompleted) {
                try {
                    contractDto.setContractFile(generateContract(contract));
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error generando el archivo de contrato");
                }
            }
            return contractDto;
        }).toList();
    }

    @Override
    @Transactional
    public ContractDto getAllContractData(UUID contractId) {
        Contract contract = findContractById(contractId);
        ContractDto contractDto = contractEntityToDto(contract);

        contractDto.setClientAttachments(contract.getClient().getAttachments()
                .stream().map(attachment -> {
                    AttachmentDto attachmentDto = modelMapper.map(attachment, AttachmentDto.class);
                    attachmentDto.setContent(attachmentUtil.getBase64FromPathReference(attachment.getPathReference()));
                    return attachmentDto;
                }).toList());

        Optional<Payment> currentPayment = paymentService.findCurrentPayment(contract, LocalDate.now());

        currentPayment.ifPresent(payment -> contractDto.setPaymentDto(modelMapper.map(payment, PaymentDto.class)));

        try {
            contractDto.setContractFile(generateContract(contract));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error generando el archivo de contrato");
        }

        return contractDto;
    }

    @Override
    public void approveAttachments(UUID contractId) {
        Contract contract = findContractById(contractId);
        contract.getStepStatuses().put(ContractStep.DOCUMENT_APPROVAL, true);
        contractRepo.save(contract);
    }

    @Override
    public void approveContract(UUID contractId) {
        Contract contract = findContractById(contractId);

        contract.getStepStatuses().forEach((step, status) -> {
            if (!step.equals(ContractStep.CLIENT_APPROVAL) && !status) {
                throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "No se pudo aprobar el contrato, faltan pasos para continuar");
            }
        });

        contract.getStepStatuses().put(ContractStep.CLIENT_APPROVAL, true);
        contract.setActive(true);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStartDate(LocalDate.now());
        sendInsuranceActivatedMail(contract.getClient().getUser().getEmail());

        contractRepo.save(contract);
    }

    @Override
    public void approvePayment(UUID contractId) {
        Contract contract = findContractById(contractId);
        contract.getStepStatuses().put(ContractStep.PAYMENT_APPROVAL, true);
        sendPaymentApprovedMail(contract.getClient().getUser().getEmail());
        contractRepo.save(contract);
    }

    @Override
    public void rejectAttachments(UUID contractId, ObservationMessageDto observationMessage) {
        Contract contract = findContractById(contractId);
        contract.getStepStatuses().put(ContractStep.DOCUMENT_APPROVAL, false);
        sendRejectAttachmentsMail(contract.getClient().getUser().getEmail(), observationMessage);
        contractRepo.save(contract);
    }

    @Override
    public List<ContractDto> findUnpaidContracts() {
        List<Contract> allContracts = contractRepo.findAll();

        LocalDate today = LocalDate.now();

        return allContracts.stream()
                .filter(contract -> {

                    boolean isPaymentStepIncomplete =
                            !Boolean.TRUE.equals(contract.getStepStatuses().getOrDefault(ContractStep.PAYMENT_APPROVAL, false));


                    LocalDate startDate = contract.getStartDate();
                    if (startDate == null) return false;

                    boolean isExpiredPayment = false;
                    switch (contract.getInsurance().getPaymentPeriod()) {
                        case MONTHLY -> isExpiredPayment = startDate.plusMonths(1).isBefore(today);
                        case YEARLY -> isExpiredPayment = startDate.plusYears(1).isBefore(today);
                    }

                    return isPaymentStepIncomplete || isExpiredPayment;
                })
                .map(this::contractEntityToDto)
                .toList();
    }

    @Override
    public List<ContractDto> findContractsExpiringSoon() {

        LocalDate today = LocalDate.now();
        LocalDate limitDate = today.plusDays(15);

        List<Contract> allContracts = contractRepo.findAll();

        return allContracts.stream()
                .filter(contract -> {
                    if ((contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.PENDING)
                            || contract.getStartDate() == null) {
                        return false;
                    }

                    LocalDate estimatedEndDate = switch (contract.getInsurance().getPaymentPeriod()) {
                        case MONTHLY -> contract.getStartDate().plusMonths(1);
                        case YEARLY -> contract.getStartDate().plusYears(1);
                    };
                    return !estimatedEndDate.isBefore(today) && !estimatedEndDate.isAfter(limitDate);
                })
                .map(this::contractEntityToDto)
                .toList();
    }

    @Override
    public List<ContractDto> findExpiredContracts() {
        LocalDate today = LocalDate.now();

        return contractRepo.findAll().stream()
                .filter(contract -> {
                    if ((contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.PENDING)
                            || contract.getStartDate() == null) {
                        return false;
                    }

                    LocalDate estimatedEndDate = switch (contract.getInsurance().getPaymentPeriod()) {
                        case MONTHLY -> contract.getStartDate().plusMonths(1);
                        case YEARLY -> contract.getStartDate().plusYears(1);
                    };


                    return estimatedEndDate.isBefore(today);
                })
                .map(this::contractEntityToDto)
                .toList();
    }

    @Override
    public List<ContractDto> findPendingContracts() {
        return contractRepo.findAll().stream()
                .filter(contract -> contract.getStatus() == ContractStatus.PENDING)
                .map(this::contractEntityToDto)
                .toList();
    }

    @Override
    public ContractFileDto generateContractPDF(UUID contractId) {
        Contract contract = findContractById(contractId);
        try {
            return ContractFileDto.builder().content(generateContract(contract)).build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el PDF del contrato");
        }
    }

    private ContractDto contractEntityToDto(Contract contract) {
        ContractDto dto = mapper.map(contract, ContractDto.class);
        dto.setInsuranceId(contract.getInsurance().getId());
        dto.setClientId(contract.getClient().getId());

        dto.setBeneficiaries(contract.getBeneficiaries().stream()
                .map(beneficiary -> modelMapper.map(beneficiary, BeneficiaryDto.class)).toList());
        return dto;
    }

    private Contract contractDtoToEntity(ContractDto dto) {
        Contract contract = mapper.map(dto, Contract.class);
        contract.setInsurance(findInsuranceById(dto.getInsuranceId()));
        contract.setClient(findClientById(dto.getClientId()));

        List<Beneficiary> beneficiaries = dto.getBeneficiaries().stream()
                .map(beneficiaryDto -> {
                    Beneficiary beneficiary = modelMapper.map(beneficiaryDto, Beneficiary.class);
                    beneficiary.setContract(contract);
                    return beneficiary;
                }).toList();

        contract.setBeneficiaries(beneficiaries);
        return contract;
    }

    private Contract findContractById(UUID contractId) {
        return contractRepo.findById(contractId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el contrato"));
    }

    private Insurance findInsuranceById(UUID id) {
        return insuranceRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el seguro"));
    }

    private Client findClientById(UUID id) {
        return clientRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el cliente"));
    }

    private void sendMailForUploadDocumentStatus(String email) {
        String subject = "Documentación requerida para continuar tu proceso en FRAM Seguros Ecuador";
        String htmlContent = "<div style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>Documentos pendientes</h2>"
                + "<p>Estimado cliente,</p>"
                + "<p>Para continuar con el proceso de contratación de tu seguro, es necesario que subas los documentos requeridos a nuestra plataforma.</p>"
                + "<p>Por favor, accede a tu cuenta y carga los archivos solicitados lo antes posible.</p>"
                + "<p>Si necesitas ayuda o tienes alguna consulta, no dudes en contactarnos.</p>"
                + "<br>"
                + "<p>Atentamente,<br><strong>FRAM Seguros Ecuador</strong></p>"
                + "</div>";

        try {
            mailgunService.sendHtmlMessage(email, subject, htmlContent);
        } catch (UnirestException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el correo de recordatorio de documentos");
        }
    }

    private void sendRejectAttachmentsMail(String email, ObservationMessageDto observationMessage) {
        String subject = "Documentación rechazada - Acciones requeridas para continuar tu proceso en FRAM Seguros Ecuador";

        String htmlContent = "<div style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>Revisión de documentos</h2>"
                + "<p>Estimado cliente,</p>"
                + "<p>Tras revisar los documentos que subiste, identificamos observaciones que debes corregir para continuar con el proceso de contratación de tu seguro.</p>"
                + "<p><strong>Observación:</strong> " + observationMessage.getObservation() + "</p>"
                + "<p>Te pedimos volver a ingresar a la plataforma y subir nuevamente los documentos corregidos.</p>"
                + "<p>Si necesitas ayuda o tienes alguna consulta, no dudes en contactarnos.</p>"
                + "<br>"
                + "<p>Atentamente,<br><strong>FRAM Seguros Ecuador</strong></p>"
                + "</div>";

        try {
            mailgunService.sendHtmlMessage(email, subject, htmlContent);
        } catch (UnirestException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el correo de rechazo de documentos");
        }
    }

    private void sendPaymentApprovedMail(String email) {
        String subject = "Pago aprobado - Gracias por confiar en FRAM Seguros Ecuador";

        String htmlContent = "<div style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>¡Pago aprobado!</h2>"
                + "<p>Estimado cliente,</p>"
                + "<p>Hemos recibido y aprobado tu pago satisfactoriamente. Gracias por confiar en nosotros.</p>"
                + "<p>Tu póliza de seguro ya está siendo procesada y recibirás la documentación correspondiente en breve.</p>"
                + "<p>Si necesitas más información, puedes ingresar a tu cuenta o comunicarte con nuestro equipo de atención al cliente.</p>"
                + "<br>"
                + "<p>Atentamente,<br><strong>FRAM Seguros Ecuador</strong></p>"
                + "</div>";

        try {
            mailgunService.sendHtmlMessage(email, subject, htmlContent);
        } catch (UnirestException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el correo de confirmación de pago");
        }
    }

    private void sendInsuranceActivatedMail(String email) {
        String subject = "Tu seguro ha sido activado - FRAM Seguros Ecuador";

        String htmlContent = "<div style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>¡Tu seguro está activo!</h2>"
                + "<p>Estimado cliente,</p>"
                + "<p>Te informamos que tu póliza de seguro ha sido activada exitosamente.</p>"
                + "<p>Ya puedes acceder a todos los beneficios y coberturas de tu plan contratado. La documentación correspondiente estará disponible en tu cuenta.</p>"
                + "<p>Gracias por confiar en nosotros para proteger lo que más valoras.</p>"
                + "<br>"
                + "<p>Atentamente,<br><strong>FRAM Seguros Ecuador</strong></p>"
                + "</div>";

        try {
            mailgunService.sendHtmlMessage(email, subject, htmlContent);
        } catch (UnirestException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el correo de activación del seguro");
        }
    }

    private void addConditionAmount(Contract contract) {
        double paymentAmount = contract.getInsurance().getPaymentAmount();
        int addedPercentage = contract.getClient().getConditions().stream()
                .map(Condition::getAddedPercentage)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);

        double paymentWithCondition = paymentAmount * (1 + (addedPercentage / 100.0));
        contract.setTotalPaymentAmount(BigDecimal.valueOf(paymentWithCondition));
    }

    private Map<ContractStep, Boolean> initialStepStatuses(Contract contract) {
        Client client = contract.getClient();

        Map<ContractStep, Boolean> steps = new EnumMap<>(ContractStep.class);

        if (client.getAttachments().isEmpty()) {
            steps.put(ContractStep.UPLOAD_DOCUMENTS, false);
        } else {
            steps.put(ContractStep.UPLOAD_DOCUMENTS, true);

        }
        steps.put(ContractStep.DOCUMENT_APPROVAL, false);
        steps.put(ContractStep.PAYMENT_APPROVAL, false);
        steps.put(ContractStep.CLIENT_APPROVAL, false);
        return steps;
    }

    private void checkClientInsurance(Client client, UUID insuranceId) {
        boolean exists = client.getContracts().stream()
                .anyMatch(contract -> contract.getInsurance().getId().equals(insuranceId));

        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cliente ya tiene un contrato con este seguro");
        }
    }

    @Override
    public List<ClientContractDto> getContractsGroupedByClient() {
        List<Contract> allContracts = contractRepo.findAll();

        Map<Client, List<Contract>> grouped = allContracts.stream()
                .collect(Collectors.groupingBy(Contract::getClient));


        return grouped.entrySet().stream()
                .map(entry -> {
                    ClientDto clientDto = mapper.map(entry.getKey(), ClientDto.class);
                    List<ContractDto> contractDtos = entry.getValue().stream()
                            .map(this::contractEntityToDto)
                            .toList();
                    ClientContractDto dto = new ClientContractDto();
                    dto.setClient(clientDto);
                    dto.setContracts(contractDtos);
                    return dto;
                })
                .toList();
    }


    public String generateContract(Contract contract) throws Exception {
        Client client = contract.getClient();
        Insurance insurance = contract.getInsurance();
        List<Beneficiary> beneficiaries = contract.getBeneficiaries();
        List<Condition> conditions = new ArrayList<>(client.getConditions());

        BigDecimal percentSum = conditions.stream()
                .map(c -> BigDecimal.valueOf(c.getAddedPercentage()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal base = BigDecimal.valueOf(insurance.getPaymentAmount());

        BigDecimal adjustedPayment = base
                .multiply(BigDecimal.ONE.add(percentSum.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));

        Context ctx = new Context(Locale.forLanguageTag("es"));
        ctx.setVariable("contract", contract);
        ctx.setVariable("client", client);
        ctx.setVariable("insurance", insurance);
        ctx.setVariable("beneficiaries", beneficiaries);
        ctx.setVariable("conditions", conditions);
        ctx.setVariable("percentSum", percentSum);
        ctx.setVariable("adjustedPayment", adjustedPayment);
        ctx.setVariable("paymentFrequency", insurance.getPaymentPeriod().getLabel());
        ctx.setVariable("totalPayment", adjustedPayment);

        String htmlContent = templateEngine.process("contract", ctx);

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent,
                Objects.requireNonNull(this.getClass().getResource("/templates/")).toString());
        renderer.layout();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            renderer.createPDF(baos);
            return attachmentUtil.getBase64FromByte(baos.toByteArray());
        }
    }
}
