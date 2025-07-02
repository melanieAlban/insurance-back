package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.InsuranceDto;
import com.fram.insurance_manager.entity.Benefit;
import com.fram.insurance_manager.entity.Contract;
import com.fram.insurance_manager.entity.Insurance;
import com.fram.insurance_manager.enums.InsuranceType;
import com.fram.insurance_manager.enums.PaymentPeriod;
import com.fram.insurance_manager.repository.BenefitRepository;
import com.fram.insurance_manager.repository.InsuranceRepository;
import com.fram.insurance_manager.service.impl.InsuranceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.MappingException;
import org.modelmapper.convention.MatchingStrategies;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsuranceServiceTest {

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private BenefitRepository benefitRepository;

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private InsuranceServiceImpl insuranceService;

    private Insurance insurance;
    private InsuranceDto dto;
    private UUID id;

    @BeforeEach
    void init() {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        id = UUID.randomUUID();

        insurance = Insurance.builder()
                .id(id)
                .name("Plan Salud")
                .type(InsuranceType.HEALTH)
                .description("Cobertura básica")
                .coverage(100_000.0)
                .deductible(200.0)
                .paymentAmount(29.9)
                .paymentPeriod(PaymentPeriod.MONTHLY)
                .active(true)
                .build();

        dto = modelMapper.map(insurance, InsuranceDto.class);
    }

    @Test
    void getAll_shouldReturnList() {
        when(insuranceRepository.findAll()).thenReturn(List.of(insurance));

        List<InsuranceDto> result = insuranceService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(id);
        verify(insuranceRepository).findAll();
    }

    @Test
    void getById_existing_returnsDto() {
        when(insuranceRepository.findById(id)).thenReturn(Optional.of(insurance));

        InsuranceDto result = insuranceService.getById(id);

        assertThat(result.getName()).isEqualTo("Plan Salud");
        verify(insuranceRepository).findById(id);
    }

    @Test
    void getById_notFound_throws404() {
        when(insuranceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> insuranceService.getById(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void update_existing_modifiesAndReturnsDto() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Beneficio simulado
        Benefit benefit = new Benefit();
        benefit.setId(UUID.randomUUID());

        // Simular DTO entrante
        InsuranceDto patch = dto.toBuilder()
                .coverage(150_000.0)
                .name("Plan Premium")
                .benefits(Set.of(benefit))
                .build();

        // Simular entidad existente en BD
        insurance.setBenefits(new HashSet<>()); // evitar NPE
        when(insuranceRepository.findById(id)).thenReturn(Optional.of(insurance));
        when(insuranceRepository.findByName(anyString())).thenReturn(Optional.empty());

        // Simular que el beneficio existe en BD
        when(benefitRepository.findById(benefit.getId())).thenReturn(Optional.of(benefit));

        // Entidad modificada esperada al guardar
        Insurance modified = insurance.toBuilder()
                .coverage(150_000.0)
                .name("Plan Premium")
                .benefits(Set.of(benefit))
                .build();

        when(insuranceRepository.save(any(Insurance.class))).thenReturn(modified);

        // Act
        InsuranceDto result = insuranceService.update(id, patch);

        // Assert
        assertThat(result.getCoverage()).isEqualTo(150_000.0);
        assertThat(result.getName()).isEqualTo("Plan Premium");
        verify(insuranceRepository).save(any(Insurance.class));
    }

    @Test
    void update_notFound_throws404() {
        when(insuranceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> insuranceService.update(id, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateStatusById_changesStatus() {
        when(insuranceRepository.findById(id)).thenReturn(Optional.of(insurance));

        Insurance inactive = insurance.toBuilder().active(false).build();
        when(insuranceRepository.save(any())).thenReturn(inactive);

        InsuranceDto result = insuranceService.updateStatusById(id, false);

        assertThat(result.isActive()).isFalse();
        verify(insuranceRepository).save(any());
    }

    @Test
    void save_newName_persistsAndReturns() {
        when(insuranceRepository.findByName("Plan Salud")).thenReturn(Optional.empty());
        when(insuranceRepository.save(any(Insurance.class))).thenReturn(insurance);

        InsuranceDto saved = insuranceService.save(dto);

        assertThat(saved.getId()).isEqualTo(id);
        verify(insuranceRepository).save(any());
    }

    @Test
    void save_duplicateName_throwsConflict() {
        when(insuranceRepository.findByName("Plan Salud")).thenReturn(Optional.of(insurance));

        assertThatThrownBy(() -> insuranceService.save(dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void delete_existing_removes() {
        when(insuranceRepository.findById(id)).thenReturn(Optional.of(insurance));
        doNothing().when(insuranceRepository).delete(insurance);

        insuranceService.delete(id);

        verify(insuranceRepository).delete(insurance);
    }

    @Test
    void delete_notFound_throws404() {
        when(insuranceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> insuranceService.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getBenefits_returnsAllBenefits() {
        // Arrange
        Benefit benefit1 = new Benefit();
        benefit1.setId(UUID.randomUUID());
        benefit1.setName("Benefit 1");

        Benefit benefit2 = new Benefit();
        benefit2.setId(UUID.randomUUID());
        benefit2.setName("Benefit 2");

        List<Benefit> benefits = List.of(benefit1, benefit2);
        when(benefitRepository.findAll()).thenReturn(benefits);

        // Act
        List<Benefit> result = insuranceService.getBenefits();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(benefits);
        verify(benefitRepository).findAll();
    }

    @Test
    void save_withBenefits_validatesBenefits() {
        // Arrange
        Benefit benefit = new Benefit();
        benefit.setId(UUID.randomUUID());
        benefit.setName("Test Benefit");

        Set<Benefit> benefits = new HashSet<>();
        benefits.add(benefit);

        InsuranceDto dtoWithBenefits = dto.toBuilder()
                .benefits(benefits)
                .build();

        when(benefitRepository.findById(benefit.getId())).thenReturn(Optional.of(benefit));
        when(insuranceRepository.findByName(dtoWithBenefits.getName())).thenReturn(Optional.empty());
        when(insuranceRepository.save(any(Insurance.class))).thenReturn(insurance);

        // Act
        InsuranceDto result = insuranceService.save(dtoWithBenefits);

        // Assert
        assertThat(result).isNotNull();
        verify(benefitRepository).findById(benefit.getId());
    }

    @Test
    void save_withNonExistentBenefit_throws404() {
        // Arrange
        Benefit benefit = new Benefit();
        benefit.setId(UUID.randomUUID());
        benefit.setName("Non-existent Benefit");

        Set<Benefit> benefits = new HashSet<>();
        benefits.add(benefit);

        InsuranceDto dtoWithBenefits = dto.toBuilder()
                .benefits(benefits)
                .build();

        when(benefitRepository.findById(benefit.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> insuranceService.save(dtoWithBenefits))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verify(benefitRepository).findById(benefit.getId());
        verifyNoInteractions(insuranceRepository);
    }

    @Test
    void update_sameNameDifferentId_throwsConflict() {
        // Arrange
        UUID otherId = UUID.randomUUID();
        Insurance otherInsurance = insurance.toBuilder()
                .id(otherId)
                .build();

        when(insuranceRepository.findById(id)).thenReturn(Optional.of(insurance));
        when(insuranceRepository.findByName(dto.getName())).thenReturn(Optional.of(otherInsurance));

        // Act & Assert
        assertThatThrownBy(() -> insuranceService.update(id, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        verify(insuranceRepository).findById(id);
        verify(insuranceRepository).findByName(dto.getName());
        verifyNoMoreInteractions(insuranceRepository);
    }

    @Test
    void save_repositorySaveThrowsException_propagatesException() {
        // Arrange
        when(insuranceRepository.findByName(dto.getName())).thenReturn(Optional.empty());
        when(insuranceRepository.save(any(Insurance.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> insuranceService.save(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(insuranceRepository).findByName(dto.getName());
        verify(insuranceRepository).save(any(Insurance.class));
    }

    @Test
    void getAll_modelMapperThrowsException_propagatesException() {
        // Arrange
        when(insuranceRepository.findAll()).thenReturn(List.of(insurance));
        doThrow(new RuntimeException("Mapping error"))
                .when(modelMapper).map(any(Insurance.class), eq(InsuranceDto.class));

        // Act & Assert
        assertThatThrownBy(() -> insuranceService.getAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Mapping error");

        verify(insuranceRepository).findAll();
    }

    @Test
    void getAll_modelMapperReturnsNull_includesNullInResult() {
        // Arrange
        when(insuranceRepository.findAll()).thenReturn(List.of(insurance));
        doReturn(null).when(modelMapper).map(any(Insurance.class), eq(InsuranceDto.class));

        // Act
        List<InsuranceDto> result = insuranceService.getAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNull();
        verify(insuranceRepository).findAll();
        verify(modelMapper, times(2)).map(any(Insurance.class), eq(InsuranceDto.class));
    }
}
