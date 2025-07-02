package com.fram.insurance_manager.dto;

import com.fram.insurance_manager.enums.AttachmentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private UUID id;
    @NotNull(message = "El tipo es obligatorio")
    private AttachmentType attachmentType;
    @NotNull(message = "El nombre del archivo es obligatorio")
    private String fileName;
    @NotNull(message = "El contenido es obligatorio")
    private String content;
}
