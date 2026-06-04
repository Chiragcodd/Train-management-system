package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.CoachType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CoachAvailabilityDto {
    private CoachType coachType;
    private int totalSeats;
    private int availableSeats;
    private int bookedSeats;
    private int waitlistedCount;
    private BigDecimal baseFare;
    private BigDecimal ratePerKm;
}