package com.fram.insurance_manager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "benefit")
public class Benefit {
    @Id
    @GeneratedValue
    @NotNull(message = "El id del beneficio es obligatorio")
    private UUID id;
    private String name;
    private String description;
}
