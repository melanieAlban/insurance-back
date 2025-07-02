package com.fram.insurance_manager.repository;

import com.fram.insurance_manager.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Ref;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {
    List<RefundRequest> findByDateGreaterThanEqual(LocalDate date);

    List<RefundRequest> findAllByContract_Client_Id(UUID clientId);
}
