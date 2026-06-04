package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.CoachType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BookingRequestDto {

    @NotNull(message = "Train ID is required")
    private Long trainId;

    @NotNull(message = "Coach type is required")
    private CoachType coachType;

    @NotBlank(message = "From station code is required")
    private String fromStationCode;

    @NotBlank(message = "To station code is required")
    private String toStationCode;

    @NotNull(message = "Travel date is required")
    @FutureOrPresent(message = "Travel date cannot be in the past")
    private LocalDate travelDate;

    @NotEmpty(message = "At least 1 passenger is required")
    @Size(max = 6, message = "A maximum of 4 passengers are allowed per booking")
    @Valid
    private List<PassengerRequestDto> passengers;
}