package com.chirag.train_management_system.service;

import com.chirag.train_management_system.enums.CoachType;
import com.chirag.train_management_system.enums.TrainType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareCalculatorService {

    private static final BigDecimal MINIMUM_FARE = new BigDecimal("10.00");

    public BigDecimal calculateFare(CoachType coachType,
                                    TrainType trainType,
                                    double distanceKm) {

        if (distanceKm <= 0) {
            throw new IllegalArgumentException(
                    "Distance must be greater than 0 km.");
        }

        BigDecimal distance    = BigDecimal.valueOf(distanceKm);
        BigDecimal baseFare    = coachType.getBaseFare();
        BigDecimal ratePerKm   = coachType.getRatePerKm();
        BigDecimal multiplier  = trainType.getPriceMultiplier();

        BigDecimal raw = baseFare.add(ratePerKm.multiply(distance));

        BigDecimal fare = raw.multiply(multiplier)
                             .setScale(2, RoundingMode.HALF_UP);

        return fare.compareTo(MINIMUM_FARE) < 0 ? MINIMUM_FARE : fare;
    }

    public String fareBreakdown(CoachType coachType,
                                TrainType trainType,
                                double distanceKm) {
        BigDecimal fare = calculateFare(coachType, trainType, distanceKm);
        return String.format(
                "%s | %s | %.1f km → ₹%.2f per passenger "
                + "(base ₹%s + ₹%s/km × multiplier %.2f)",
                coachType, trainType, distanceKm, fare,
                coachType.getBaseFare(), coachType.getRatePerKm(),
                trainType.getPriceMultiplier().doubleValue());
    }
}