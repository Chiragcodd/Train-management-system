package com.chirag.train_management_system.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TrainSeatAvailabilityDto {
    private Long trainId;
    private String trainName;
    private String trainNumber;
    private LocalDate travelDate;
    private List<CoachAvailabilityDto> coaches;
}