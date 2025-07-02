package com.fram.insurance_manager.controller;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fram.insurance_manager.dto.InsuranceDto;
import com.fram.insurance_manager.entity.Benefit;
import com.fram.insurance_manager.service.InsuranceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/insurance")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class InsuranceController {
    private final InsuranceService insuranceService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public InsuranceDto save(@Valid @RequestBody InsuranceDto insuranceDto) {
        return insuranceService.save(insuranceDto);
    }

    @GetMapping
    public List<InsuranceDto> getAll() {
        return insuranceService.getAll();
    }

    @GetMapping("/{id}")
    public InsuranceDto getById(@PathVariable UUID id) {
        return insuranceService.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public InsuranceDto update(@PathVariable UUID id, @RequestBody InsuranceDto insuranceDto) {
        return insuranceService.update(id, insuranceDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/status/{id}")
    public InsuranceDto updateActiveStatus(@PathVariable UUID id, @RequestParam boolean status) {
        return insuranceService.updateStatusById(id, status);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        insuranceService.delete(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("benefits")
    public List<Benefit> getBenefits() {
        return insuranceService.getBenefits();
    }
}