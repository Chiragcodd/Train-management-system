package com.chirag.train_management_system.controller;

import com.chirag.train_management_system.dto.StationRequestDto;
import com.chirag.train_management_system.dto.StationResponseDto;
import com.chirag.train_management_system.service.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationResponseDto> addStation(
            @Valid @RequestBody StationRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stationService.addStation(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<StationResponseDto> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<StationResponseDto> getStationByCode(@PathVariable String code) {
        return ResponseEntity.ok(stationService.getStationByCode(code));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<StationResponseDto>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    // ✅ NEW: Station name se search — autocomplete ke liye
    // Example: GET /api/stations/search?q=mumb
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<StationResponseDto>> searchByName(
            @RequestParam String q) {
        return ResponseEntity.ok(stationService.searchByName(q));
    }

    // ✅ NEW: City ke saare stations
    // Example: GET /api/stations/city/Mumbai
    @GetMapping("/city/{city}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<StationResponseDto>> getStationsByCity(
            @PathVariable String city) {
        return ResponseEntity.ok(stationService.getStationsByCity(city));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationResponseDto> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody StationRequestDto dto) {
        return ResponseEntity.ok(stationService.updateStation(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }
}