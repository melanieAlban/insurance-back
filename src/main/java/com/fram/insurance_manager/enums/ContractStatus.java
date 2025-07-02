package com.fram.insurance_manager.enums;

public enum ContractStatus {
    PENDING,   // mientras no esté completo
    ACTIVE,    // contrato vigente
    CANCELLED,
    REJECTED_BY_CLIENT,
    EXPIRED
}