package com.fram.insurance_manager.controller;

import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.ClientDto;
import com.fram.insurance_manager.enums.UserRol;
import com.fram.insurance_manager.service.ClientService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/client")
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ClientController {
    private ClientService clientService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @PostMapping
    public ClientDto save(@Valid @RequestBody ClientDto clientDto) {
        clientDto.getUser().setRol(UserRol.CLIENT);
        return clientService.save(clientDto);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @PutMapping
    public ClientDto update(@Valid @RequestBody ClientDto clientDto) {
        return clientService.update(clientDto.getId(), clientDto);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        clientService.delete(id);
    }

    @GetMapping("/{id}")
    public ClientDto getById(@PathVariable UUID id) {
        return clientService.getById(id);
    }

    @GetMapping()
    public List<ClientDto> getAll() {
        return clientService.getAll();
    }

    @GetMapping("/identification/{id}")
    public ClientDto getByIdentification(@PathVariable String id) {
        return clientService.getByIdentification(id);
    }

    @PostMapping("attachments/{clientId}")
    public void uploadDocumentAttachments(@PathVariable UUID clientId,
                                          @Valid @RequestBody List<AttachmentDto> attachments) {
        clientService.uploadDocumentAttachments(clientId, attachments);
    }

    @PostMapping("/test/base64")
    public ResponseEntity<String> convertToBase64(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }

        try {
            byte[] fileBytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(fileBytes);
            return ResponseEntity.ok(base64);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo procesar el archivo");
        }
    }
}
