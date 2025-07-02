package com.fram.insurance_manager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "condition")
public class Condition {
    @Id
    @GeneratedValue
    private UUID id;
    private String name;
    private String description;
    private Integer addedPercentage;

    @ManyToMany(mappedBy = "conditions")
    private List<Client> client = new ArrayList<>();
}
