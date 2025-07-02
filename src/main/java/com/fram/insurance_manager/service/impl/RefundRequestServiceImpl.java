package com.fram.insurance_manager.service.impl;

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
import com.fram.insurance_manager.service.RefundRequestService;
import com.fram.insurance_manager.util.AttachmentUtil;
import com.fram.insurance_manager.util.UserUtil;
import com.mashape.unirest.http.exceptions.UnirestException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundRequestServiceImpl implements RefundRequestService {
    private final RefundRequestRepository refundRequestRepository;
    private final ContractRepository contractRepository;
    private final AttachmentRepository attachmentRepository;
    private final ModelMapper modelMapper;
    private final MailgunService mailgunService;
    private final AttachmentUtil attachmentUtil;
    private final UserUtil userUtil;
    private final UserRepository userRepository;

    @Override
    public List<RefundRequestDto> getRefundRequests() {
        User user = this.userRepository.findById(userUtil.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Client client = user.getClient();
        UserRol rol = user.getRol();

        if (rol.equals(UserRol.CLIENT) && client != null) {
            return refundRequestRepository.findAllByContract_Client_Id(client.getId()).stream().map(this::refundRequestToDto).toList();
        } else {
            return refundRequestRepository.findAll().stream().map(this::refundRequestToDto).toList();
        }
    }

    @Override
    public RefundRequestDto getRefundRequest(UUID refundRequestId) {
        return refundRequestToDto(findRefundRequestById(refundRequestId));
    }

    @Override
    public RefundRequestDto save(RefundRequestDto refundRequestDto) {
        Contract contract = findContractById(refundRequestDto.getContractId());

        if (!contract.getStatus().equals(ContractStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "El contrato no está activo");
        }

        refundRequestDto.getAttachments().forEach(attachmentUtil::validateAttachment);

        RefundRequest refundRequest = modelMapper.map(refundRequestDto, RefundRequest.class);
        refundRequest.setDate(LocalDate.now());
        refundRequest.setContract(contract);
        refundRequest.setStatus(RefundRequestStatus.NEW);
        refundRequest.setAmountPaid(refundRequestDto.getPaidAmount());
        refundRequest.setCoveredAmount(calculateCoveredAmount(contract.getPayments(), contract.getInsurance().getCoverage(), refundRequestDto.getPaidAmount()));

        refundRequestDto.getAttachments().forEach(attachmentDto -> {
            Attachment attachment = attachmentDtoToEntity(attachmentDto);

            Path path = attachmentUtil.saveAttachmentOnDiskBase64(attachmentDto.getContent(),
                    attachmentDto.getFileName(), "refund_" + contract.getClient().getLastName(), null);
            attachment.setPathReference(path.toString());
            attachment.setRefundRequest(refundRequest);
            attachment.setAttachmentType(AttachmentType.REFUND_REQUEST);
            refundRequest.getAttachments().add(attachment);
        });

        RefundRequest saved = refundRequestRepository.save(refundRequest);
        sendRefundRequestCreatedMail(contract.getClient().getUser().getEmail());

        return refundRequestToDto(saved);
    }

    @Override
    @Transactional
    public void reject(UUID refundRequestId, String reason) {
        RefundRequest refundRequest = findRefundRequestById(refundRequestId);
        refundRequest.setStatus(RefundRequestStatus.REJECTED);
        refundRequest.setObservation("Razón de rechazo: ".concat(reason));
        refundRequest.setCoveredAmount(0.0);

        sendRefundRequestRejectedMail(refundRequest.getContract().getClient().getUser().getEmail(), reason);

        refundRequestRepository.save(refundRequest);
    }

    @Override
    public void approve(UUID refundRequestId) {
        RefundRequest refundRequest = findRefundRequestById(refundRequestId);
        refundRequest.setStatus(RefundRequestStatus.APPROVED);
        refundRequest.setObservation("Reembolso aprobado correctamente");
        refundRequest.setDescription("");
        sendRefundRequestApprovedMail(refundRequest.getContract().getClient().getUser().getEmail(), refundRequest.getCoveredAmount());
        refundRequestRepository.save(refundRequest);
    }

    @Override
    @Transactional
    public void updateAttachments(UUID refundRequestId, List<AttachmentDto> attachmentDtos) {
        RefundRequest refundRequest = findRefundRequestById(refundRequestId);

        if (attachmentDtos == null || attachmentDtos.isEmpty() || attachmentDtos.size() > 3) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Mínimo 1 documento y máximo 3");
        }

        Set<UUID> existentAttachmentIds = attachmentDtos.stream().map(AttachmentDto::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        refundRequest.getAttachments().removeIf(
                attachment -> !existentAttachmentIds.contains(attachment.getId())
        );

        attachmentDtos.forEach(attachmentDto -> {
            attachmentUtil.validateAttachment(attachmentDto);

            Attachment attachment = attachmentRepository.findById(attachmentDto.getId()).orElse(null);

            if (attachment == null) {
                attachment = modelMapper.map(attachmentDto, Attachment.class);
                Path path = attachmentUtil.saveAttachmentOnDiskBase64(
                        attachmentDto.getContent(),
                        attachmentDto.getFileName(),
                        "refund_".concat(refundRequest.getContract().getClient().getLastName()), null);
                attachment.setPathReference(path.toString());
                attachment.setRefundRequest(refundRequest);
                refundRequest.getAttachments().add(attachment);
            }
        });

        refundRequestRepository.save(refundRequest);
    }

    private Contract findContractById(UUID contractId) {
        return contractRepository.findById(contractId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato no encontrado"));
    }

    private RefundRequest findRefundRequestById(UUID refundRequestId) {
        return refundRequestRepository.findById(refundRequestId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud de reembolso no encontrado"));
    }

    private Attachment attachmentDtoToEntity(AttachmentDto dto) {
        return modelMapper.map(dto, Attachment.class);
    }

    RefundRequestDto refundRequestToDto(RefundRequest refundRequest) {
        RefundRequestDto dto = modelMapper.map(refundRequest, RefundRequestDto.class);
        dto.setContractId(refundRequest.getContract().getId());

        List<AttachmentDto> attachmentDtos = refundRequest.getAttachments().stream()
                .map(attachment -> {
                    AttachmentDto ad = new AttachmentDto();
                    ad.setFileName(attachment.getFileName());
                    ad.setAttachmentType(attachment.getAttachmentType());

                    // Validate pathReference before passing it to attachmentUtil
                    if (attachment.getPathReference() != null && !attachment.getPathReference().isBlank()) {
                        ad.setContent(attachmentUtil.getBase64FromPathReference(attachment.getPathReference()));
                    } else {
                        ad.setContent(null); // Set content as null if pathReference is invalid
                        // Optionally log a warning here
                    }

                    return ad;
                }).toList();

        dto.setAttachments(attachmentDtos);
        return dto;
    }

    private void sendRefundRequestCreatedMail(String email) {
        String subject = "Solicitud de reembolso recibida - FRAM Seguros Ecuador";

        String htmlContent = "<div style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>¡Tu solicitud de reembolso ha sido registrada!</h2>"
                + "<p>Estimado cliente,</p>"
                + "<p>Hemos recibido tu solicitud de reembolso y está siendo procesada por nuestro equipo.</p>"
                + "<p>Te notificaremos por este medio una vez se haya revisado y validado la información proporcionada.</p>"
                + "<p>Puedes consultar el estado de tu solicitud ingresando a tu cuenta en nuestra plataforma.</p>"
                + "<br>"
                + "<p>Atentamente,<br><strong>FRAM Seguros Ecuador</strong></p>"
                + "</div>";

        try {
            mailgunService.sendHtmlMessage(email, subject, htmlContent);
        } catch (UnirestException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el correo de confirmación de solicitud de reembolso");
        }
    }

    private void sendRefundRequestRejectedMail(String email, String reason) {
        String subject = "Solicitud de reembolso rechazada - FRAM Seguros Ecuador";

        String htmlContent = "<div style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>Tu solicitud de reembolso ha sido rechazada</h2>"
                + "<p>Estimado cliente,</p>"
                + "<p>Lamentamos informarte que tu solicitud de reembolso ha sido rechazada por el siguiente motivo:</p>"
                + "<blockquote style='color: #b00020; font-style: italic;'>" + reason + "</blockquote>"
                + "<p>Si tienes dudas o deseas más información, por favor contáctanos por nuestros canales oficiales.</p>"
                + "<br>"
                + "<p>Atentamente,<br><strong>FRAM Seguros Ecuador</strong></p>"
                + "</div>";

        try {
            mailgunService.sendHtmlMessage(email, subject, htmlContent);
        } catch (UnirestException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el correo de rechazo de solicitud de reembolso");
        }
    }

    private void sendRefundRequestApprovedMail(String email, Double amountRefunded) {
        String subject = "Solicitud de reembolso aprobada - FRAM Seguros Ecuador";

        String htmlContent = "<div style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>¡Tu solicitud de reembolso ha sido aprobada!</h2>"
                + "<p>Estimado cliente,</p>"
                + "<p>Nos complace informarte que tu solicitud de reembolso ha sido revisada y aprobada.</p>"
                + "<p>Monto a reembolsar: <strong>$" + amountRefunded + "</strong></p>"
                + "<p>El reembolso será procesado en los próximos días hábiles y se acreditará según el método de pago registrado.</p>"
                + "<p>Gracias por confiar en <strong>FRAM Seguros Ecuador</strong>.</p>"
                + "<br>"
                + "<p>Atentamente,<br><strong>FRAM Seguros Ecuador</strong></p>"
                + "</div>";

        try {
            mailgunService.sendHtmlMessage(email, subject, htmlContent);
        } catch (UnirestException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el correo de aprobación de reembolso");
        }
    }

    private double calculateCoveredAmount(
            List<Payment> payments,
            Double insuranceCoverage,
            Double paidAmount) {

        LocalDate lastPaymentDay = payments.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Payment::getDate))
                .map(Payment::getDate)
                .orElse(null);

        List<RefundRequest> periodRefundRequests =
                refundRequestRepository.findByDateGreaterThanEqual(lastPaymentDay);

        double actualCoverage = periodRefundRequests.stream()
                .map(RefundRequest::getCoveredAmount)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        double coverageRemaining = insuranceCoverage - actualCoverage;

        if (coverageRemaining <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya se ha agotado la cobertura para el periodo actual"
            );
        }

        return Math.min(paidAmount, coverageRemaining);
    }
}