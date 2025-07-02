package com.fram.insurance_manager.dto;

import com.fram.insurance_manager.entity.Benefit;
import com.fram.insurance_manager.enums.InsuranceType;
import com.fram.insurance_manager.enums.PaymentPeriod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class InsuranceDto {
    private UUID id;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotNull(message = "El tipo de seguro es obligatorio")
    private InsuranceType type;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    @NotNull(message = "La cobertura es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cobertura debe ser mayor que 0")
    private Double coverage;

    @NotNull(message = "El deducible es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El deducible no puede ser negativo")
    private Double deductible;

    @NotNull(message = "El monto de pago es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El monto de pago debe ser mayor que 0")
    private Double paymentAmount;

    @NotNull(message = "El período de pago es obligatorio")
    private PaymentPeriod paymentPeriod;

    @Valid
    private Set<Benefit> benefits;

    private boolean active;
}
