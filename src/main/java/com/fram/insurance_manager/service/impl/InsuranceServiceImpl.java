package com.fram.insurance_manager.service.impl;

import com.fram.insurance_manager.dto.InsuranceDto;
import com.fram.insurance_manager.entity.Benefit;
import com.fram.insurance_manager.entity.Insurance;
import com.fram.insurance_manager.repository.BenefitRepository;
import com.fram.insurance_manager.repository.InsuranceRepository;
import com.fram.insurance_manager.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InsuranceServiceImpl implements InsuranceService {
    private final InsuranceRepository insuranceRepository;
    private final BenefitRepository benefitRepository;
    private final ModelMapper modelMapper;

    @Override
    public InsuranceDto save(InsuranceDto insuranceDto) {
        checkBenefits(insuranceDto.getBenefits());

        insuranceRepository.findByName(insuranceDto.getName())
                .ifPresent(existingInsurance -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe un seguro con nombre " + insuranceDto.getName());
                });

        Insurance insurance = modelMapper.map(insuranceDto, Insurance.class);
        return insuranceToDto(insuranceRepository.save(insurance));
    }

    @Override
    public List<InsuranceDto> getAll() {
        return insuranceRepository.findAll().stream().map(this::insuranceToDto).toList();
    }

    @Override
    public InsuranceDto getById(UUID id) {
        Insurance insurance = findInsuranceById(id);
        return insuranceToDto(insurance);
    }

    @Override
    public InsuranceDto update(UUID id, InsuranceDto insuranceDto) {
        checkBenefits(insuranceDto.getBenefits());

        Insurance insurance = findInsuranceById(id);

        insuranceRepository.findByName(insuranceDto.getName())
                .ifPresent(existingInsurance -> {
                    if (!existingInsurance.getId().equals(id)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Ya existe un seguro con nombre " + insuranceDto.getName());
                    }
                });


        insurance.setName(insuranceDto.getName());
        insurance.setType(insuranceDto.getType());
        insurance.setDescription(insuranceDto.getDescription());
        insurance.setCoverage(insuranceDto.getCoverage());
        insurance.setDeductible(insuranceDto.getDeductible());
        insurance.setPaymentAmount(insuranceDto.getPaymentAmount());
        insurance.setPaymentPeriod(insuranceDto.getPaymentPeriod());
        insurance.setActive(insuranceDto.isActive());
    

        insurance.getBenefits().clear();
        insurance.getBenefits().addAll(insuranceDto.getBenefits()); 

        return insuranceToDto(insuranceRepository.save(insurance));
    }


    @Override
    public InsuranceDto updateStatusById(UUID id, boolean newStatus) {
        Insurance insurance = findInsuranceById(id);
        insurance.setActive(newStatus);
        return insuranceToDto(insuranceRepository.save(insurance));
    }

    @Override
    public void delete(UUID id) {
        insuranceRepository.delete(findInsuranceById(id));
    }

    @Override
    public List<Benefit> getBenefits() {
        return benefitRepository.findAll();
    }

    private InsuranceDto insuranceToDto(Insurance insurance) {
        return modelMapper.map(insurance, InsuranceDto.class);
    }

    private Insurance findInsuranceById(UUID id) {
        return insuranceRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el seguro"));
    }

    private void checkBenefits(Set<Benefit> benefits) {
        if (benefits == null) {
            return;
        }

        benefits.forEach(benefit -> {
            benefitRepository.findById(benefit.getId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el beneficio")
            );
        });
    }
}
