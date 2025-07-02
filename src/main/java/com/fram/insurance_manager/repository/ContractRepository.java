package com.fram.insurance_manager.repository;

import com.fram.insurance_manager.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {
    List<Contract> findAllByClientId(UUID clientId);
}
