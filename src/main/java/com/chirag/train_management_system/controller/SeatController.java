package com.chirag.train_management_system.controller;

import com.chirag.train_management_system.dto.SeatRequestDto;
import com.chirag.train_management_system.dto.SeatResponseDto;
import com.chirag.train_management_system.dto.TrainSeatAvailabilityDto;
import com.chirag.train_management_system.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SeatResponseDto>> addSeats(
            @Valid @RequestBody SeatRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatService.addSeats(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SeatResponseDto> getSeatById(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    @GetMapping("/train/{trainId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<SeatResponseDto>> getSeatsByTrain(
            @PathVariable Long trainId) {
        return ResponseEntity.ok(seatService.getSeatsByTrain(trainId));
    }

    @GetMapping("/train/{trainId}/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<SeatResponseDto>> getAvailableSeats(
            @PathVariable Long trainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate) {
        return ResponseEntity.ok(seatService.getAvailableSeats(trainId, travelDate));
    }

    @GetMapping("/train/{trainId}/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TrainSeatAvailabilityDto> getTrainSeatAvailability(
            @PathVariable Long trainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate) {
        return ResponseEntity.ok(seatService.getTrainSeatAvailability(trainId, travelDate));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSeat(@PathVariable Long id) {
        seatService.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }
}