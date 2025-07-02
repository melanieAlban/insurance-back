package com.fram.insurance_manager.util;

import com.fram.insurance_manager.config.attachment.AttachmentProperties;
import com.fram.insurance_manager.dto.AttachmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentUtilTest {

    @Mock
    private AttachmentProperties attachmentProperties;

    @InjectMocks
    private AttachmentUtil attachmentUtil;

    @TempDir
    Path tempDir;

    private String validBase64Content;
    private byte[] testFileData;

    @BeforeEach
    void setUp() throws IOException {
        // Set up valid base64 content
        testFileData = "Test file content".getBytes();
        validBase64Content = Base64.getEncoder().encodeToString(testFileData);

        // Configure mock properties with lenient to avoid "unnecessary stubbing" errors
        lenient().when(attachmentProperties.getMaxFileSize()).thenReturn("5MB");
        lenient().when(attachmentProperties.getAllowedTypes()).thenReturn(List.of("png", "jpg", "pdf"));
        lenient().when(attachmentProperties.getDirectoryPath()).thenReturn(tempDir.toString());
    }

    @Test
    void validateAttachment_validAttachment_noExceptionThrown() {
        // Arrange
        AttachmentDto validAttachment = AttachmentDto.builder()
                .fileName("test.png")
                .content(validBase64Content)
                .build();

        // Act & Assert - no exception should be thrown
        attachmentUtil.validateAttachment(validAttachment);
    }

    @Test
    void validateAttachment_invalidBase64_throwsBadRequest() {
        // Arrange
        AttachmentDto invalidAttachment = AttachmentDto.builder()
                .fileName("test.png")
                .content("invalid base64 content!")
                .build();

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> attachmentUtil.validateAttachment(invalidAttachment));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Contenido base64 inválido");
    }

    @Test
    void validateAttachment_disallowedFileType_throwsBadRequest() {
        // Arrange
        AttachmentDto invalidTypeAttachment = AttachmentDto.builder()
                .fileName("test.exe")
                .content(validBase64Content)
                .build();

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> attachmentUtil.validateAttachment(invalidTypeAttachment));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Tipo de archivo no permitido: exe");
    }

    @Test
    void validateAttachment_fileTooLarge_throwsBadRequest() {
        // Arrange
        when(attachmentProperties.getMaxFileSize()).thenReturn("1KB"); // Set max size to 1 kilobyte

        // Create a large content that exceeds 1KB
        byte[] largeData = new byte[2048]; // 2KB
        String largeBase64Content = Base64.getEncoder().encodeToString(largeData);

        AttachmentDto largeAttachment = AttachmentDto.builder()
                .fileName("test.png")
                .content(largeBase64Content)
                .build();

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> attachmentUtil.validateAttachment(largeAttachment));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("El archivo excede el tamaño máximo permitido");
    }

    @Test
    void saveAttachmentOnDiskBase64_validContent_savesFile() {
        // Arrange
        String fileName = "test.png";
        String identifier = "testUser";

        // Act
        Path result = attachmentUtil.saveAttachmentOnDiskBase64(validBase64Content, fileName, identifier, null);

        // Assert
        assertThat(result).exists();
        assertThat(result.toString()).contains(identifier);
    }

    @Test
    void saveAttachmentOnDiskBase64_withExistingPath_overwritesFile() throws IOException {
        // Arrange
        String fileName = "test.png";
        String identifier = "testUser";

        // Create an existing file
        Path existingPath = tempDir.resolve("existing_file.png");
        Files.write(existingPath, "Old content".getBytes());

        // Act
        Path result = attachmentUtil.saveAttachmentOnDiskBase64(validBase64Content, fileName, identifier, existingPath.toString());

        // Assert
        assertThat(result).exists();
        assertThat(result).isEqualTo(existingPath);
        assertThat(Files.readAllBytes(result)).isEqualTo(testFileData);
    }

    @Test
    void saveAttachmentOnDiskBase64_emptyContent_throwsPreconditionFailed() {
        // Arrange
        String fileName = "test.png";
        String identifier = "testUser";

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> attachmentUtil.saveAttachmentOnDiskBase64("", fileName, identifier, null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(exception.getReason()).contains("Contenido base64 vacío");
    }

    @Test
    void saveAttachmentOnDisk_validData_savesFile() throws IOException {
        // Arrange
        Path targetPath = tempDir.resolve("test_file.txt");

        // Act
        Path result = attachmentUtil.saveAttachmentOnDisk(testFileData, targetPath);

        // Assert
        assertThat(result).exists();
        assertThat(Files.readAllBytes(result)).isEqualTo(testFileData);
    }

    @Test
    void saveAttachmentOnDisk_emptyData_throwsPreconditionFailed() {
        // Arrange
        Path targetPath = tempDir.resolve("empty_file.txt");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> attachmentUtil.saveAttachmentOnDisk(new byte[0], targetPath));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(exception.getReason()).contains("Archivo vacío");
    }

    @Test
    void getBase64FromPathReference_validPath_returnsBase64() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("test_file.txt");
        Files.write(testFile, testFileData);

        // Act
        String result = attachmentUtil.getBase64FromPathReference(testFile.toString());

        // Assert
        assertThat(result).isEqualTo(validBase64Content);
    }

    @Test
    void getBase64FromPathReference_invalidPath_throwsInternalServerError() {
        // Arrange
        String nonExistentPath = tempDir.resolve("non_existent_file.txt").toString();

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> attachmentUtil.getBase64FromPathReference(nonExistentPath));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getReason()).contains("No se pudo leer el archivo");
    }

    @Test
    void getBase64FromByte_validData_returnsBase64() {
        // Act
        String result = attachmentUtil.getBase64FromByte(testFileData);

        // Assert
        assertThat(result).isEqualTo(validBase64Content);
    }
}
