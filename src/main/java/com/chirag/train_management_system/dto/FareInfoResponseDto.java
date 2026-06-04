package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.CoachType;
import com.chirag.train_management_system.enums.TrainType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Fare preview endpoint ka response.
 * GET /api/trains/{id}/fare?from=NDLS&to=BCT&coach=AC_3
 */
@Data
public class FareInfoResponseDto {
    private String fromStation;
    private String toStation;
    private double distanceKm;
    private CoachType coachType;
    private TrainType trainType;
    private BigDecimal baseFare;
    private BigDecimal ratePerKm;
    private BigDecimal trainTypeMultiplier;
    private BigDecimal farePerPassenger;
    private String breakdown;
}