package com.chirag.train_management_system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StationRequestDto {

    @NotBlank(message = "Station name is required")
    @Size(min = 2, max = 100, message = "Station name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Station code is required")
    @Size(min = 2, max = 10, message = "Station code must be between 2 and 10 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Station code must be uppercase letters and numbers only")
    private String code;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;
}