package com.fram.insurance_manager.controller;

import com.fram.insurance_manager.dto.ConditionDto;
import com.fram.insurance_manager.service.ConditionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/condition")
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ConditionController {

    private final ConditionService conditionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ConditionDto save(@RequestBody ConditionDto conditionDto) {
        return conditionService.save(conditionDto);
    }

    @GetMapping
    public List<ConditionDto> getAll() {
        return conditionService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConditionDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(conditionService.getById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        conditionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ConditionDto>> getConditionsByClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(conditionService.getConditionsByClient(clientId));
    }

    @PostMapping("/client/{clientId}/assign/{conditionId}")
    public ResponseEntity<List<ConditionDto>> assignConditionToClient(
            @PathVariable UUID clientId,
            @PathVariable UUID conditionId) {
        return ResponseEntity.ok(conditionService.setConditionToClient(clientId, conditionId));
    }

    @DeleteMapping("/client/{clientId}/remove/{conditionId}")
    public ResponseEntity<List<ConditionDto>> removeConditionFromClient(
            @PathVariable UUID clientId,
            @PathVariable UUID conditionId) {
        return ResponseEntity.ok(conditionService.removeConditionFromClient(clientId, conditionId));
    }
}
