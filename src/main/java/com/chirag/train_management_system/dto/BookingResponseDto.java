package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.BookingStatus;
import com.chirag.train_management_system.enums.CoachType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponseDto {
    private Long bookingId;
    private String pnrNumber;

    private String userName;
    private String trainName;
    private String trainNumber;
    private String fromStation;
    private String toStation;
    private CoachType coachType;
    private LocalDate travelDate;
    private LocalDateTime bookingDate;

    private BookingStatus status;

    private BigDecimal    totalAmount;
    private BigDecimal    refundAmount;
    private LocalDateTime paymentExpiresAt;

    private Integer       waitlistNumber;

    private LocalDateTime cancelledAt;
   
    private Double        journeyDistanceKm;
    private BigDecimal    farePerPassenger;
    private String        fareBreakdown;

    private int           confirmedCount;

    private int           waitlistedCount;

    private List<PassengerResponseDto> passengers;
}