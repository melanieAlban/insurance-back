package com.fram.insurance_manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ClientDto {

    private UUID id;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    @NotBlank(message = "El número de identificación es obligatorio")
    private String identificationNumber;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate birthDate;

    @NotNull(message = "El número de teléfono es obligatorio")
    private String phoneNumber;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    @NotBlank(message = "El género es obligatorio")
    private String gender;

    @NotBlank(message = "La ocupación es obligatoria")
    private String occupation;

    private boolean active;

    @Valid
    private SaveUserDto user;

    private Set<UUID> conditionsIds;
}
