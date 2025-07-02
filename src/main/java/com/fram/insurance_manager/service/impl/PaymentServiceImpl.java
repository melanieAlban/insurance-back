package com.fram.insurance_manager.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.PaymentDto;
import com.fram.insurance_manager.dto.PaymentUrlDto;
import com.fram.insurance_manager.entity.Attachment;
import com.fram.insurance_manager.entity.Contract;
import com.fram.insurance_manager.entity.Payment;
import com.fram.insurance_manager.enums.AttachmentType;
import com.fram.insurance_manager.enums.ContractStep;
import com.fram.insurance_manager.enums.PaymentPeriod;
import com.fram.insurance_manager.enums.PaymentType;
import com.fram.insurance_manager.repository.ContractRepository;
import com.fram.insurance_manager.repository.PaymentRepository;
import com.fram.insurance_manager.service.PaymentService;
import com.fram.insurance_manager.util.AttachmentUtil;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final AttachmentUtil attachmentUtil;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public PaymentDto save(PaymentDto paymentDto) {
        Contract contract = findContractById(paymentDto.getContractId());
        paymentDto.setAmount(Double.valueOf(String.valueOf(contract.getTotalPaymentAmount())));
        Payment payment = upsertPayment(contract, paymentDto);

        return paymentEntityToDto(paymentRepository.save(payment));
    }

    @Override
    public PaymentUrlDto createCheckoutSession(UUID contractId) throws Exception {
        Contract contract = findContractById(contractId);
        BigDecimal amount = contract.getTotalPaymentAmount(); // $19.99

        long amountInCents = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:4200/main-revision/".concat(contractId.toString()))
                .setCancelUrl("http://localhost:4200/main-revision/".concat(contractId.toString()))
                .setClientReferenceId(contractId.toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("usd").setUnitAmount(amountInCents)
                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName("Pago de seguro ".concat(contract.getInsurance().getName()))
                                                .setDescription("Código de referencia: ".concat(contract.getId().toString()))
                                                .build()).build()).build()).build();

        Session session = Session.create(params);
        return PaymentUrlDto.builder().url(session.getUrl()).build();
    }

    @Override
    public void handleStripeWebhook(String payload, String sigHeader) {
        String endpointSecret = "whsec_a04c3be39742c3182d076206f1dfb88676e3429e756337bfc7eb0dd659fba1af";

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

            if ("checkout.session.completed".equals(event.getType())) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(payload);

                String sessionId = jsonNode.get("data").get("object").get("id").asText();
                String contractId = jsonNode.get("data").get("object").get("client_reference_id").asText();


                Contract contract = findContractById(UUID.fromString(contractId));
                contract.getStepStatuses().put(ContractStep.PAYMENT_APPROVAL, true);

                Payment payment = Payment.builder()
                        .paymentType(PaymentType.CARD)
                        .amount(contract.getTotalPaymentAmount().doubleValue())
                        .date(LocalDate.now())
                        .referenceSessionId(sessionId)
                        .contract(contract)
                        .build();
                contract.getPayments().add(payment);

                paymentRepository.save(payment);
                contractRepository.save(contract);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al manejar el pago");
        }
    }

    private Payment upsertPayment(Contract contract, PaymentDto dto) {
        LocalDate today = LocalDate.now();

        return findCurrentPayment(contract, today).map(existing -> updateExistingPayment(existing, dto)).orElseGet(() -> createNewPayment(contract, dto));
    }

    private Payment createNewPayment(Contract contract, PaymentDto dto) {
        Payment payment = modelMapper.map(dto, Payment.class);
        payment.setContract(contract);

        saveOrUpdateAttachment(payment, dto.getAttachment(), null);

        return payment;
    }

    private Payment updateExistingPayment(Payment existing, PaymentDto dto) {
        String oldPath = existing.getAttachment().getPathReference();

        modelMapper.map(dto, existing);

        saveOrUpdateAttachment(existing, dto.getAttachment(), oldPath);

        return existing;
    }

    private void saveOrUpdateAttachment(Payment payment, AttachmentDto dto, String oldPath) {
        Attachment attachment = modelMapper.map(dto, Attachment.class);
        attachment.setAttachmentType(AttachmentType.PAYMENT_PROOF);

        Path stored = attachmentUtil.saveAttachmentOnDiskBase64(dto.getContent(), dto.getFileName(), payment.getContract().getId().toString().substring(0, 5), oldPath);
        attachment.setPathReference(stored.toString());
        payment.setAttachment(attachment);
    }

    public Optional<Payment> findCurrentPayment(Contract contract, LocalDate now) {
        LocalDate start = contract.getStartDate();
        PaymentPeriod period = contract.getInsurance().getPaymentPeriod();

        return contract.getPayments().stream().filter(Objects::nonNull).filter(p -> {
            LocalDate pd = p.getDate();
            if (period == PaymentPeriod.MONTHLY) {
                long monthsBetween = ChronoUnit.MONTHS.between(start, now);
                if (monthsBetween > 0) {
                    monthsBetween--;
                }
                LocalDate target = start.plusMonths(monthsBetween);
                return pd.getYear() == target.getYear() && pd.getMonth() == target.getMonth();

            } else if (period == PaymentPeriod.YEARLY) {
                long yearsBetween = ChronoUnit.YEARS.between(start, now);
                if (yearsBetween > 0) {
                    yearsBetween--;
                }
                int targetYear = start.plusYears(yearsBetween).getYear();
                return pd.getYear() == targetYear;
            }
            return false;
        }).findFirst();
    }


    private PaymentDto paymentEntityToDto(Payment payment) {
        PaymentDto dto = modelMapper.map(payment, PaymentDto.class);
        dto.setContractId(payment.getContract().getId());

        String base64Payment = attachmentUtil.getBase64FromPathReference(payment.getAttachment().getPathReference());
        dto.getAttachment().setContent(base64Payment);

        return dto;
    }

    private Contract findContractById(UUID contractId) {
        return contractRepository.findById(contractId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato no encontrado"));
    }
}
