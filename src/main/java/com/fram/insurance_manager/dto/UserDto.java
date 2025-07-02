package com.fram.insurance_manager.dto;

import com.fram.insurance_manager.enums.UserRol;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class UserDto {
    private UUID id;
    private String name;
    private String email;
    private UserRol rol;
    private boolean active;
}
