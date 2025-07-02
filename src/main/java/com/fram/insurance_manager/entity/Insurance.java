package com.fram.insurance_manager.entity;

import com.fram.insurance_manager.enums.InsuranceType;
import com.fram.insurance_manager.enums.PaymentPeriod;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "insurance")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class Insurance {
    @Id
    @GeneratedValue
    private UUID id;
    private String name;

    @Enumerated(EnumType.STRING)
    private InsuranceType type;

    private String description;
    private Double coverage;
    private Double deductible;
    private Double paymentAmount;

    @Enumerated(EnumType.STRING)
    private PaymentPeriod paymentPeriod;

    private boolean active;

    @OneToMany(mappedBy = "insurance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Contract> contracts;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(
            name = "insurance_benefit",
            joinColumns = @JoinColumn(name = "insurance_id"),
            inverseJoinColumns = @JoinColumn(name = "benefit_id")
    )
    private Set<Benefit> benefits;
}
