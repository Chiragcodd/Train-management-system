package com.chirag.train_management_system.enums;

import java.math.BigDecimal;

public enum CoachType {

    SLEEPER (new BigDecimal("30"),  new BigDecimal("0.50")),
    AC_3    (new BigDecimal("100"), new BigDecimal("1.25")),
    AC_2    (new BigDecimal("150"), new BigDecimal("1.80")),
    AC_1    (new BigDecimal("250"), new BigDecimal("3.00"));

    private final BigDecimal baseFare;
    private final BigDecimal ratePerKm;

    CoachType(BigDecimal baseFare, BigDecimal ratePerKm) {
        this.baseFare   = baseFare;
        this.ratePerKm  = ratePerKm;
    }

    public BigDecimal getBaseFare()  { return baseFare;  }
    public BigDecimal getRatePerKm() { return ratePerKm; }
}