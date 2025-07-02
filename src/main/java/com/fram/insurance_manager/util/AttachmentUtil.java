package com.fram.insurance_manager.util;

import com.fram.insurance_manager.config.attachment.AttachmentProperties;
import com.fram.insurance_manager.dto.AttachmentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentUtil {
    private final AttachmentProperties attachmentProperties;

    public void validateAttachment(AttachmentDto attachmentDto) {
        byte[] decodedBytes;

        try {
            decodedBytes = Base64.getDecoder().decode(attachmentDto.getContent());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contenido base64 inválido");
        }

        long maxSize = parseSizeToBytes(attachmentProperties.getMaxFileSize());
        if (decodedBytes.length > maxSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo excede el tamaño máximo permitido de "
                            .concat(attachmentProperties.getMaxFileSize()));
        }

        String extension = getFileExtension(attachmentDto.getFileName());
        boolean isAllowed = attachmentProperties.getAllowedTypes().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(extension));

        if (!isAllowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo no permitido: " + extension);
        }
    }

    public Path saveAttachmentOnDiskBase64(String base64Content, String fileName, String identifier, String pathReference) {
        if (base64Content == null || base64Content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Contenido base64 vacío");
        }

        byte[] fileData;
        try {
            fileData = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contenido base64 inválido");
        }

        Path targetPath;
        if (pathReference != null && !pathReference.isBlank()) {
            targetPath = Paths.get(pathReference);
        } else {
            String ext = (fileName != null && fileName.contains("."))
                    ? fileName.substring(fileName.lastIndexOf(".")) : "";
            String generatedFileName = UUID.randomUUID().toString().substring(0, 5) + "_" + identifier + ext;
            targetPath = Paths.get(attachmentProperties.getDirectoryPath(), generatedFileName);
        }

        return saveAttachmentOnDisk(fileData, targetPath);
    }

    public Path saveAttachmentOnDisk(byte[] fileData, Path path) {
        if (fileData == null || fileData.length == 0) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Archivo vacío");
        }

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, fileData);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar el archivo");
        }

        return path;
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1);
        }
        return "";
    }

    private long parseSizeToBytes(String size) {
        size = size.toUpperCase().trim();
        if (size.endsWith("MB")) {
            return Long.parseLong(size.replace("MB", "").trim()) * 1024 * 1024;
        } else if (size.endsWith("KB")) {
            return Long.parseLong(size.replace("KB", "").trim()) * 1024;
        } else {
            return Long.parseLong(size); // assume bytes
        }
    }

    public String getBase64FromPathReference(String pathReference) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(pathReference));
            return Base64.getEncoder().encodeToString(data);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el archivo");
        }
    }

    public String getBase64FromByte(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
