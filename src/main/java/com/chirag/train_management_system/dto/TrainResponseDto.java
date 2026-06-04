package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.TrainStatus;
import com.chirag.train_management_system.enums.TrainType;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

@Data
public class TrainResponseDto {
    private Long id;
    private String trainName;
    private String trainNumber;
    private TrainStatus status;
    // ✅ NEW
    private TrainType trainType;
    private Set<DayOfWeek> runningDays;
    private List<RouteResponseDto> routes;
}