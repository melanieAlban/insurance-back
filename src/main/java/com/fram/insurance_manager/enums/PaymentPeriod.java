package com.fram.insurance_manager.enums;

public enum PaymentPeriod {
  MONTHLY("Mensual"),
  YEARLY("Anual");

  private final String label;

  PaymentPeriod(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}