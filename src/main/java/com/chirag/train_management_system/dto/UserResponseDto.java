package com.chirag.train_management_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserResponseDto {
    private Long id;
    private String name;
    private String email;
    private String username;
    private String mobileNumber;
    private List<String> roles;
}
