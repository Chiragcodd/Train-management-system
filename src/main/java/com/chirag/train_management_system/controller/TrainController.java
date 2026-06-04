package com.chirag.train_management_system.controller;

import com.chirag.train_management_system.dto.FareInfoResponseDto;
import com.chirag.train_management_system.dto.TrainRequestDto;
import com.chirag.train_management_system.dto.TrainResponseDto;
import com.chirag.train_management_system.enums.CoachType;
import com.chirag.train_management_system.enums.TrainStatus;
import com.chirag.train_management_system.service.TrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trains")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrainResponseDto> addTrain(@Valid @RequestBody TrainRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainService.addTrain(dto));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TrainResponseDto>> addMultipleTrains(
            @Valid @RequestBody List<TrainRequestDto> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainService.addMultipleTrains(dtos));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<TrainResponseDto>> searchTrains(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate) {
        return ResponseEntity.ok(trainService.searchTrains(from, to, travelDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TrainResponseDto> getTrainById(@PathVariable Long id) {
        return ResponseEntity.ok(trainService.getTrainById(id));
    }

    @GetMapping("/number/{trainNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TrainResponseDto> getTrainByNumber(@PathVariable String trainNumber) {
        return ResponseEntity.ok(trainService.getTrainByNumber(trainNumber));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<TrainResponseDto>> getAllTrains(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(trainService.getAllTrains(page, size));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrainResponseDto> updateTrain(
            @PathVariable Long id, @Valid @RequestBody TrainRequestDto dto) {
        return ResponseEntity.ok(trainService.updateTrain(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrainResponseDto> updateTrainStatus(
            @PathVariable Long id, @RequestParam TrainStatus status) {
        return ResponseEntity.ok(trainService.updateTrainStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTrain(@PathVariable Long id) {
        trainService.deleteTrain(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ NEW: Fare preview — booking se pehle kitna lagega check karo
    // Example: GET /api/trains/1/fare?from=NDLS&to=BCT&coach=AC_3
    @GetMapping("/{id}/fare")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FareInfoResponseDto> getFarePreview(
            @PathVariable Long id,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam CoachType coach) {
        return ResponseEntity.ok(trainService.getFarePreview(id, from, to, coach));
    }
}