package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.ClientContractDto;
import com.fram.insurance_manager.dto.ContractDto;
import com.fram.insurance_manager.dto.ContractFileDto;
import com.fram.insurance_manager.dto.ObservationMessageDto;

import java.util.List;
import java.util.UUID;

public interface ContractService {
    ContractDto save(ContractDto dto);

    ContractDto findById(UUID id);

    List<ContractDto> findAll();

    ContractDto getAllContractData(UUID contractId);

    void approveAttachments(UUID contractId);

    void approveContract(UUID contractId);

    void approvePayment(UUID contractId);

    void rejectAttachments(UUID contractId, ObservationMessageDto observationMessage);

    List<ContractDto> findUnpaidContracts();

    List<ContractDto> findContractsExpiringSoon();

    List<ContractDto> findExpiredContracts();

    List<ContractDto> findPendingContracts();

    List<ClientContractDto> getContractsGroupedByClient();

    ContractFileDto generateContractPDF(UUID contractId);
}