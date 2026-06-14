package com.chirag.train_management_system.controller;

import com.chirag.train_management_system.dto.ChangePasswordRequestDto;
import com.chirag.train_management_system.dto.ResetPasswordRequestDto;
import com.chirag.train_management_system.dto.SendOtpRequestDto;
import com.chirag.train_management_system.dto.UserRequestDto;
import com.chirag.train_management_system.dto.UserResponseDto;
import com.chirag.train_management_system.dto.UserUpdateRequestDto;
import com.chirag.train_management_system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(
            @Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.register(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserResponseDto> getUserById(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(userService.getUserById(id, authentication));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateUser(id, dto, authentication));
    }

    // ── Way 1 — Change Password ────────────────────────────────
    @PutMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequestDto dto,
            Authentication authentication) {
        userService.changePassword(id, dto.getCurrentPassword(),
                dto.getNewPassword(), authentication);
        return ResponseEntity.ok(Map.of("message",
                "Password changed successfully"));
    }

    // ── Way 2 — Send OTP ──────────────────────────────────────
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(
            @Valid @RequestBody SendOtpRequestDto dto) {
        userService.sendForgotPasswordOtp(dto.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "OTP sent to your registered email"));
    }

    // ── Way 2 — Reset Password ────────────────────────────────
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto dto) {
        userService.resetPasswordWithOtp(
                dto.getEmail(), dto.getOtp(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of("message",
                "Password reset successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}