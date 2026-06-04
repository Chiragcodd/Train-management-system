package com.chirag.train_management_system.service;

import com.chirag.train_management_system.dto.*;
import com.chirag.train_management_system.entity.Station;
import com.chirag.train_management_system.exception.*;
import com.chirag.train_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;
    private final RouteRepository   routeRepository;

    // ─────────────────────────────────────────────────────────
    //  ADD
    // ─────────────────────────────────────────────────────────
    @Transactional
    public StationResponseDto addStation(StationRequestDto dto) {
        String code = dto.getCode().trim().toUpperCase();

        if (stationRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Station", "code", code);
        }

        Station station = new Station();
        station.setName(dto.getName().trim());
        station.setCode(code);
        station.setCity(dto.getCity().trim());

        log.info("Station added | {} ({})", station.getName(), station.getCode());
        return toDto(stationRepository.save(station));
    }

    // ─────────────────────────────────────────────────────────
    //  READ
    // ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public StationResponseDto getStationById(Long id) {
        return toDto(findById(id));
    }

    @Transactional(readOnly = true)
    public StationResponseDto getStationByCode(String code) {
        return toDto(stationRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Station", "code", code)));
    }

    @Transactional(readOnly = true)
    public List<StationResponseDto> getAllStations() {
        return stationRepository.findAll()
                .stream().map(this::toDto).toList();
    }

    // Partial name search — autocomplete ke liye
    // Example: "mumb" → Mumbai Central, Mumbai CST...
    @Transactional(readOnly = true)
    public List<StationResponseDto> searchByName(String query) {
        if (query == null || query.isBlank()) {
            throw new BookingValidationException(
                    "Search query cannot be empty.");
        }
        return stationRepository
                .findByNameContainingIgnoreCase(query.trim())
                .stream().map(this::toDto).toList();
    }

    // City ke saare stations
    @Transactional(readOnly = true)
    public List<StationResponseDto> getStationsByCity(String city) {
        if (city == null || city.isBlank()) {
            throw new BookingValidationException("City name cannot be empty.");
        }
        return stationRepository
                .findByCityIgnoreCase(city.trim())
                .stream().map(this::toDto).toList();
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────────────────
    @Transactional
    public StationResponseDto updateStation(Long id, StationRequestDto dto) {
        Station station = findById(id);
        String newCode = dto.getCode().trim().toUpperCase();

        // Code change ho raha hai aur already kisi aur station ka hai
        stationRepository.findByCode(newCode)
                .filter(s -> !s.getId().equals(id))
                .ifPresent(s -> {
                    throw new DuplicateResourceException(
                            "Station", "code", newCode);
                });

        station.setName(dto.getName().trim());
        station.setCode(newCode);
        station.setCity(dto.getCity().trim());

        log.info("Station updated | {} ({})", station.getName(), station.getCode());
        return toDto(stationRepository.save(station));
    }

    // ─────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────
    @Transactional
    public void deleteStation(Long id) {
        if (!stationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Station", id);
        }

        // Station kisi route mein use ho rahi hai to delete mat karo
        if (routeRepository.existsByStationId(id)) {
            throw new BookingValidationException(
                    "Cannot delete station. It is part of one or more train routes.");
        }

        stationRepository.deleteById(id);
        log.info("Station deleted | id={}", id);
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS — other services use karte hain
    // ─────────────────────────────────────────────────────────
    public Station findById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station", id));
    }

    public Station findByCode(String code) {
        return stationRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Station", "code", code));
    }

    private StationResponseDto toDto(Station station) {
        StationResponseDto dto = new StationResponseDto();
        dto.setId(station.getId());
        dto.setName(station.getName());
        dto.setCode(station.getCode());
        dto.setCity(station.getCity());
        return dto;
    }
}