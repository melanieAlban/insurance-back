package com.fram.insurance_manager.entity;

import com.fram.insurance_manager.enums.RefundRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "refund_request")
public class RefundRequest {
    @Id
    @GeneratedValue
    private UUID id;

    private String refundType; // Emergencia, receta medica, consulta medica, cirugia, etc.
    private String description;
    private LocalDate date;
    private String observation;
    private Double amountPaid;
    private Double coveredAmount;

    @Enumerated(EnumType.STRING)
    private RefundRequestStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "refundRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;
}
