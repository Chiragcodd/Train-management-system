package com.chirag.train_management_system.service;

import com.chirag.train_management_system.dto.UserRequestDto;
import com.chirag.train_management_system.dto.UserResponseDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository    userRepository;
    private final RoleRepository    roleRepository;
    private final PasswordEncoder   passwordEncoder;
    private final BookingRepository bookingRepository; 
    @Transactional
    public UserResponseDto register(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail()))
            throw new DuplicateResourceException(
                    "User", "email", dto.getEmail());

        if (userRepository.existsByUsername(dto.getUsername()))
            throw new DuplicateResourceException(
                    "User", "username", dto.getUsername());

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role", "name", "ROLE_USER"));

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
            Long id, UserRequestDto dto, Authentication authentication) {

        User user = findById(id);
        checkOwnershipOrAdmin(user.getUsername(), authentication);

        if (!user.getEmail().equals(dto.getEmail())
                && userRepository.existsByEmail(dto.getEmail()))
            throw new DuplicateResourceException(
                    "User", "email", dto.getEmail());

        if (!user.getUsername().equals(dto.getUsername())
                && userRepository.existsByUsername(dto.getUsername()))
            throw new DuplicateResourceException(
                    "User", "username", dto.getUsername());

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setMobileNumber(dto.getMobileNumber());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return toDto(userRepository.save(user));
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

    // ─── Helpers ──────────────────────────────────────────

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
        boolean isOwner = authentication.getName()
                .equals(resourceOwnerUsername);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
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
        dto.setRoles(user.getRoles().stream()
                .map(Role::getName).toList());
        return dto;
    }
}
