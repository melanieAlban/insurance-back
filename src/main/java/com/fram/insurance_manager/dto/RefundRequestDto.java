package com.fram.insurance_manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fram.insurance_manager.entity.Attachment;
import com.fram.insurance_manager.entity.Contract;
import com.fram.insurance_manager.enums.RefundRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundRequestDto {
    private UUID id;

    @NotBlank(message = "El tipo de reembolso es obligatorio")
    private String refundType;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate date;

    private String observation;
    private Double paidAmount;
    private Double coveredAmount;

    @Enumerated(EnumType.STRING)
    private RefundRequestStatus status;

    @Valid
    @Size(min = 1, max = 3, message = "Debe adjuntar entre 1 y 3 archivos")
    @Builder.Default
    private List<AttachmentDto> attachments = new ArrayList<>();

    @NotNull(message = "El contrato es obligatorio")
    private UUID contractId;
}
