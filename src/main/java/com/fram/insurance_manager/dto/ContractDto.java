package com.fram.insurance_manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fram.insurance_manager.enums.ContractStatus;
import com.fram.insurance_manager.enums.ContractStep;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractDto {
    private UUID id;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    @Builder.Default
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private ContractStatus status = ContractStatus.PENDING;

    @Builder.Default
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean active = false;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal totalPaymentAmount;

    @NotNull(message = "El seguro es obligatorio")
    private UUID insuranceId;

    @NotNull(message = "El cliente es obligatorio")
    private UUID clientId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private ClientDto client;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private InsuranceDto insurance;

    @Builder.Default
    private List<BeneficiaryDto> beneficiaries = new ArrayList<>();

    @Builder.Default
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<AttachmentDto> clientAttachments = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private PaymentDto paymentDto;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String contractFile;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Builder.Default
    private Map<ContractStep, Boolean> stepStatuses = new EnumMap<>(ContractStep.class);
}