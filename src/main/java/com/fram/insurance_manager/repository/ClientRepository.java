package com.fram.insurance_manager.repository;

import com.fram.insurance_manager.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Client findByIdentificationNumber(String identificationNumber);
}
