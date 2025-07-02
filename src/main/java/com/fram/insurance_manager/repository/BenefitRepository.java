package com.fram.insurance_manager.repository;

import com.fram.insurance_manager.entity.Benefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BenefitRepository extends JpaRepository<Benefit, UUID> {
}
