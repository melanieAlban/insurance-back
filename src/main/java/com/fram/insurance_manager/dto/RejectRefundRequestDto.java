package com.fram.insurance_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RejectRefundRequestDto {
    @NotNull(message = "El id de la solicitud de reembolso es obligatoria")
    private UUID id;
    @NotBlank(message = "La razón es obligatoria")
    private String reason;
}
