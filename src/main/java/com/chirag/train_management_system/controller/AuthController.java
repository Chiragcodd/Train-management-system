package com.chirag.train_management_system.controller;

import com.chirag.train_management_system.dto.LoginRequestDto;
import com.chirag.train_management_system.dto.LoginResponseDto;
import com.chirag.train_management_system.entity.User;
import com.chirag.train_management_system.security.JwtUtil;
import com.chirag.train_management_system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        User user = userService.findByUsername(dto.getUsername());
        String token = jwtUtil.generateToken(user.getUsername());
        String role = user.getRoles().isEmpty() ? "" : user.getRoles().get(0).getName();

        return ResponseEntity.ok(new LoginResponseDto(token, user.getUsername(), role));
    }
}