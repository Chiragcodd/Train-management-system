package com.chirag.train_management_system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class SeatRequestDto {

    @NotNull(message = "Train ID is required")
    private Long trainId;

    @NotEmpty(message = "At least one coach configuration is required")
    @Valid
    private List<CoachRequestDto> coaches;
}