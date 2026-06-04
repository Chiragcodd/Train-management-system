package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.TrainType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

@Data
public class TrainRequestDto {

    @NotBlank(message = "Train name is required")
    @Size(min = 2, max = 100, message = "Train name must be between 2 and 100 characters")
    private String trainName;

    @NotBlank(message = "Train number is required")
    @Pattern(regexp = "^[0-9]{5}$", message = "Train number must be exactly 5 digits")
    private String trainNumber;

    // ✅ NEW: Kon kon se din chalti hai
    // Example: ["MONDAY", "WEDNESDAY", "FRIDAY"]
    @NotEmpty(message = "Running days are required (e.g. MONDAY, WEDNESDAY)")
    private Set<DayOfWeek> runningDays;

    // ✅ NEW: Train ka type
    @NotNull(message = "Train type is required")
    private TrainType trainType;

    @NotEmpty(message = "Route must have at least 2 stops")
    @Size(min = 2, message = "Route must have at least 2 stops")
    @Valid
    private List<RouteRequestDto> routes;
}