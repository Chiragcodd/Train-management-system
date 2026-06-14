package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.CoachType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SeatResponseDto {
    private Long      id;
    private int       seatNumber;
    private CoachType coachType;
    private Long      trainId;
    private String    trainName;
    private BigDecimal ratePerKm;
    private BigDecimal baseFare;
}