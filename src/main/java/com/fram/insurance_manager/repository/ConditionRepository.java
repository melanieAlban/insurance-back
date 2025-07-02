package com.fram.insurance_manager.repository;

import com.fram.insurance_manager.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, UUID> {
    Condition findByName(String name);
    List<Condition> findAllByClientId(UUID clientId);
}
