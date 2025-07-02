package com.fram.insurance_manager.enums;

public enum InsuranceType {
  LIFE("Seguro de vida"),
  HEALTH("Seguro de salud");

  private final String label;

  InsuranceType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}