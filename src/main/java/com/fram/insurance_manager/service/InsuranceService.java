package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.InsuranceDto;
import com.fram.insurance_manager.entity.Benefit;

import java.util.List;
import java.util.UUID;

public interface InsuranceService {
    List<InsuranceDto> getAll();

    InsuranceDto getById(UUID id);

    InsuranceDto save(InsuranceDto insuranceDto);

    InsuranceDto update(UUID id, InsuranceDto insuranceDto);

    InsuranceDto updateStatusById(UUID id, boolean newStatus);

    void delete(UUID id);

    List<Benefit> getBenefits();
}
