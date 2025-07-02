package com.fram.insurance_manager.controller;

import com.fram.insurance_manager.dto.ClientContractDto;
import com.fram.insurance_manager.dto.ContractDto;
import com.fram.insurance_manager.dto.ContractFileDto;
import com.fram.insurance_manager.dto.ObservationMessageDto;
import com.fram.insurance_manager.service.ContractService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("contract")
@SecurityRequirement(name = "bearerAuth")
public class ContractController {
    private final ContractService contractService;

    @PostMapping
    public ContractDto save(@Valid @RequestBody ContractDto dto) {
        return contractService.save(dto);
    }

    @GetMapping
    public List<ContractDto> getAll() {
        return contractService.findAll();
    }

    @GetMapping("{id}")
    public ContractDto getById(@PathVariable UUID id) {
        return contractService.findById(id);
    }

    @GetMapping("data/{contractId}")
    public ContractDto getAllContractData(@PathVariable UUID contractId) {
        return contractService.getAllContractData(contractId);
    }

    @PostMapping("approve-attachments/{contractId}")
    public void approveAttachments(@PathVariable UUID contractId) {
        contractService.approveAttachments(contractId);
    }

    @PostMapping("reject-attachments/{contractId}")
    public void rejectAttachments(@PathVariable UUID contractId, @RequestBody ObservationMessageDto observationMessage) {
        contractService.rejectAttachments(contractId, observationMessage);
    }

    @PostMapping("approve-contract/{contractId}")
    public void approveContract(@PathVariable UUID contractId) {
        contractService.approveContract(contractId);
    }

    @PostMapping("approve-payment/{contractId}")
    public void approvePayment(@PathVariable UUID contractId) {
        contractService.approvePayment(contractId);
    }

    @GetMapping("/unpaid")
    public List<ContractDto> getUnpaidContracts() {
        return contractService.findUnpaidContracts();
    }

    @GetMapping("/expiring-soon")
    public List<ContractDto> getContractsExpiringSoon() {
        return contractService.findContractsExpiringSoon();
    }

    @GetMapping("/expired")
    public List<ContractDto> getExpiredContracts() {
        return contractService.findExpiredContracts();
    }

    @GetMapping("/pending")
    public List<ContractDto> getPendingContracts() {
        return contractService.findPendingContracts();
    }

    @GetMapping("/grouped-by-client")
    public ResponseEntity<List<ClientContractDto>> getContractsGroupedByClient() {
        return ResponseEntity.ok(contractService.getContractsGroupedByClient());
    }

    @GetMapping("/{contractId}/pdf")
    public ContractFileDto getContractPdf(@PathVariable UUID contractId) {
        return contractService.generateContractPDF(contractId);
    }
}
