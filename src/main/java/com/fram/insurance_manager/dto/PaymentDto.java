package com.fram.insurance_manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fram.insurance_manager.entity.Attachment;
import com.fram.insurance_manager.entity.Contract;
import com.fram.insurance_manager.enums.PaymentType;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;
    @NotNull(message = "La forma de pago es obligatoria")
    private PaymentType paymentType;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Double amount;
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;
    @NotNull(message = "El contrato es obligatorio")
    private UUID contractId;
    @Valid
    private AttachmentDto attachment;
}
