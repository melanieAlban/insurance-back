package com.fram.insurance_manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConditionDto {
    private UUID id;
    private String name;
    private String description;
    private Integer addedPercentage;
}
