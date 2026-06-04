package com.chirag.train_management_system.enums;

import java.math.BigDecimal;

public enum TrainType {
    LOCAL       (new BigDecimal("0.80")),
    MAIL        (new BigDecimal("0.90")),
    EXPRESS     (new BigDecimal("1.00")),
    SUPERFAST   (new BigDecimal("1.10")),
    DURONTO     (new BigDecimal("1.25")),
    SHATABDI    (new BigDecimal("1.30")),
    RAJDHANI    (new BigDecimal("1.40")),
    VANDE_BHARAT(new BigDecimal("1.50"));

    private final BigDecimal priceMultiplier;

    TrainType(BigDecimal priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public BigDecimal getPriceMultiplier() { return priceMultiplier; }
}