package com.chirag.train_management_system.dto;

import lombok.Data;

@Data
public class StationResponseDto {
    private Long id;
    private String name;
    private String code;
    private String city;
}
