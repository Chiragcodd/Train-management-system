package com.chirag.train_management_system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalTime;

@Data
public class RouteRequestDto {

    @NotNull(message = "Station ID is required")
    private Long stationId;

    @Min(value = 1, message = "Stop order must start from 1")
    private int stopOrder;

    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    @Min(value = 0, message = "Distance cannot be negative")
    private double distanceFromOrigin;
}