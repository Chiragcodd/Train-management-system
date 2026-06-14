package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.PassengerStatus;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PassengerResponseDto {
    private Long   id;
    private String name;
    private int    age;
    private String gender;


    private int    seatNumber;
    private String coachType;

    private PassengerStatus passengerStatus;

    private Integer waitlistPosition;

    private BigDecimal fare;
}