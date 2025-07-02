package com.fram.insurance_manager.dto;

import com.fram.insurance_manager.enums.UserRol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SaveUserDto {
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @Email(message = "El correo no es válido")
    @NotBlank(message = "El correo es obligatorio")
    private String email;

    private String password;

    private UserRol rol;

    private boolean active;
}
