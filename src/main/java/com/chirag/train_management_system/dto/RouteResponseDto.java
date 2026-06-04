package com.chirag.train_management_system.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class RouteResponseDto {
    private Long id;
    private int stopOrder;
    private String stationName;
    private String stationCode;
    private LocalTime arrivalTime;
    private LocalTime departureTime;
    private double distanceFromOrigin;
}
