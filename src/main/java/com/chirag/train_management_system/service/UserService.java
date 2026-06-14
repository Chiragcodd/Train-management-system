package com.chirag.train_management_system.service;

import com.chirag.train_management_system.dto.UserRequestDto;
import com.chirag.train_management_system.dto.UserResponseDto;
import com.chirag.train_management_system.dto.UserUpdateRequestDto;
import com.chirag.train_management_system.entity.Role;
import com.chirag.train_management_system.entity.User;
import com.chirag.train_management_system.exception.BookingValidationException;
import com.chirag.train_management_system.exception.CustomAccessDeniedException;
import com.chirag.train_management_system.exception.DuplicateResourceException;
import com.chirag.train_management_system.exception.ResourceNotFoundException;
import com.chirag.train_management_system.repository.BookingRepository;
import com.chirag.train_management_system.repository.RoleRepository;
import com.chirag.train_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_MAX = 1_000_000;
    private static final String OTP_FORMAT = "%0" + OTP_LENGTH + "d";
    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public UserResponseDto register(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail()))
            throw new DuplicateResourceException("User", "email", dto.getEmail());

        if (userRepository.existsByUsername(dto.getUsername()))
            throw new DuplicateResourceException("User", "username", dto.getUsername());

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_USER"));

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setMobileNumber(dto.getMobileNumber());
        user.setRoles(List.of(userRole));

        return toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id, Authentication authentication) {
        User user = findById(id);
        checkOwnershipOrAdmin(user.getUsername(), authentication);
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        return userRepository.findAll(
                PageRequest.of(page, safeSize, Sort.by("name")))
                .map(this::toDto);
    }

    @Transactional
    public UserResponseDto updateUser(
            Long id, UserUpdateRequestDto dto, Authentication authentication) {

        User user = findById(id);
        checkOwnershipOrAdmin(user.getUsername(), authentication);

        if (!user.getEmail().equals(dto.getEmail())
                && userRepository.existsByEmail(dto.getEmail()))
            throw new DuplicateResourceException("User", "email", dto.getEmail());

        if (!user.getUsername().equals(dto.getUsername())
                && userRepository.existsByUsername(dto.getUsername()))
            throw new DuplicateResourceException("User", "username", dto.getUsername());

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setMobileNumber(dto.getMobileNumber());

        // if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
        //     user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // }

        return toDto(userRepository.save(user));
    }

    // ── Way 1 — Change Password (logged in user) ──────────────
    @Transactional
    public void changePassword(Long id, String currentPassword,
            String newPassword, Authentication authentication) {

        User user = findById(id);
        checkOwnershipOrAdmin(user.getUsername(), authentication);

        // Current password verify karo
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BookingValidationException("Current password is incorrect");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BookingValidationException(
                    "New password cannot be same as current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ── Way 2 — Forgot Password Step 1: OTP bhejo ─────────────
    @Transactional
    public void sendForgotPasswordOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", email));

        // 6-digit OTP generate karo (0..999999)
        String otp = String.format(OTP_FORMAT, RNG.nextInt(OTP_MAX));

        // OTP save karo — OTP_EXPIRY_MINUTES minutes valid
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        userRepository.save(user);

        // Email bhejo
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("TrainMS — Password Reset OTP");
        message.setText(
                "Hello " + user.getName() + ",\n\n" +
                        "Your OTP for password reset is: " + otp + "\n\n" +
                        "This OTP is valid for " + OTP_EXPIRY_MINUTES + " minutes.\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "— TrainMS Team");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send OTP email to {}", email, ex);
            throw new BookingValidationException("Failed to send OTP email. Please try again later.");
        }
    }

    // ── Way 2 — Forgot Password Step 2: OTP verify + new password ──
    @Transactional
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", email));

        // OTP check karo
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new BookingValidationException("Invalid OTP");
        }

        // OTP expiry check karo
        if (user.getOtpExpiry() == null ||
                LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new BookingValidationException(
                    "OTP has expired. Please request a new one");
        }

        // New password set karo
        user.setPassword(passwordEncoder.encode(newPassword));

        // OTP clear karo
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id))
            throw new ResourceNotFoundException("User", id);

        long activeBookings = bookingRepository
                .countActiveBookingsByUserId(id, LocalDate.now());

        if (activeBookings > 0) {
            throw new BookingValidationException(
                    "Cannot delete user. They have "
                            + activeBookings
                            + " upcoming active booking(s). "
                            + "Please cancel all bookings first.");
        }

        userRepository.deleteById(id);
    }

    // ─── Helpers ──────────────────────────────────────────────
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "username", username));
    }

    private void checkOwnershipOrAdmin(String resourceOwnerUsername,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new CustomAccessDeniedException();
        }
        boolean isOwner = authentication.getName().equals(resourceOwnerUsername);
        boolean isAdmin = authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isOwner && !isAdmin)
            throw new CustomAccessDeniedException();
    }

    public UserResponseDto toDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setMobileNumber(user.getMobileNumber());
        dto.setRoles(user.getRoles() == null ? List.of()
                : user.getRoles().stream()
                        .map(Role::getName).toList());
        return dto;
    }
}