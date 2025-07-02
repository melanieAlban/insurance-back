package com.fram.insurance_manager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationMessageDto {
    @NotBlank(message = "El mensaje no puede estar vacío")
    private String observation;
}
