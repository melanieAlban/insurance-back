package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.ConditionDto;

import java.util.List;
import java.util.UUID;

public interface ConditionService {
    List<ConditionDto> getAll();
    ConditionDto getById(UUID id);
    ConditionDto save(ConditionDto conditionDto);
    void delete(UUID id);
    List<ConditionDto> getConditionsByClient(UUID clientId);
    List<ConditionDto> setConditionToClient(UUID clientId, UUID conditionId);
    List<ConditionDto> removeConditionFromClient(UUID clientId, UUID conditionId);
}
