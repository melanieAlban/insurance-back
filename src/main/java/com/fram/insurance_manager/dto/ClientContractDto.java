package com.fram.insurance_manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientContractDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private ClientDto client;

    @Builder.Default
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<ContractDto> contracts = new ArrayList<>();
}
