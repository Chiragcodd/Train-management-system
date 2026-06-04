package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.CoachType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CoachRequestDto {

    @NotNull(message = "Coach type is required")
    private CoachType coachType;

    @Min(value = 1, message = "Seat count must be at least 1")
    @Max(value = 100, message = "Cannot add more than 100 seats at once per coach")
    private int count;

}