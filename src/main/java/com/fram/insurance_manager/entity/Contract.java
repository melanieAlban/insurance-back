package com.fram.insurance_manager.entity;

import com.fram.insurance_manager.enums.ContractStatus;
import com.fram.insurance_manager.enums.ContractStep;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "contract")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Contract {
    @Id
    @GeneratedValue
    private UUID id;
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;
    private BigDecimal totalPaymentAmount; // monto total considerando condicion del cliente (discapacidad, enfermedad, edad)

    private boolean active; // solo es true cuando status es ACTIVE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_id", nullable = false)
    private Insurance insurance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Builder.Default
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Beneficiary> beneficiaries = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contract_steps", joinColumns = @JoinColumn(name = "contract_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "step")
    @Column(name = "completed")
    private Map<ContractStep, Boolean> stepStatuses = new EnumMap<>(ContractStep.class);
}
