package com.chirag.train_management_system.service;

import com.chirag.train_management_system.dto.*;
import com.chirag.train_management_system.entity.*;
import com.chirag.train_management_system.enums.TrainStatus;
import com.chirag.train_management_system.exception.*;
import com.chirag.train_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_STOPS     = 2;

    private final TrainRepository       trainRepository;
    private final StationRepository     stationRepository;
    private final RouteRepository       routeRepository;
    private final BookingRepository     bookingRepository;
    private final FareCalculatorService fareCalculator;  

    @Transactional
    public TrainResponseDto addTrain(TrainRequestDto dto) {
        return createTrain(dto);
    }

    @Transactional
    public List<TrainResponseDto> addMultipleTrains(List<TrainRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BookingValidationException("Train list cannot be empty");
        }
        return dtos.stream().map(this::createTrain).toList();
    }

    private TrainResponseDto createTrain(TrainRequestDto dto) {
        if (trainRepository.existsByTrainNumber(dto.getTrainNumber())) {
            throw new DuplicateResourceException("Train", "number", dto.getTrainNumber());
        }
        validateRoute(dto.getRoutes());

        Train train = new Train();
        train.setTrainName(dto.getTrainName());
        train.setTrainNumber(dto.getTrainNumber());
        train.setRunningDays(dto.getRunningDays());
        train.setTrainType(dto.getTrainType());
        train.setStatus(TrainStatus.ACTIVE);

        Train saved = trainRepository.save(train);
        List<Route> routes = buildRoutes(dto.getRoutes(), saved);
        routeRepository.saveAll(routes);
        saved.setRoutes(routes);

        return toDto(saved);
    }

    @Transactional
    public TrainResponseDto updateTrainStatus(Long id, TrainStatus status) {
        Train train = findById(id);
        if (train.getStatus() == status) {
            throw new BookingValidationException("Already in same status");
        }
        if (status == TrainStatus.INACTIVE || status == TrainStatus.CANCELLED) {
            long count = bookingRepository.countFutureActiveBookingsByTrainId(id, LocalDate.now());
            if (count > 0) {
                throw new BookingValidationException(
                        "Cannot " + status.name().toLowerCase()
                        + " train. It has " + count + " upcoming active booking(s).");
            }
        }
        train.setStatus(status);
        return toDto(trainRepository.save(train));
    }

    @Transactional(readOnly = true)
    public TrainResponseDto getTrainById(Long id) {
        return toDto(findById(id));
    }

    @Transactional(readOnly = true)
    public TrainResponseDto getTrainByNumber(String number) {
        Train train = trainRepository.findByTrainNumberWithRoutes(number)
                .orElseThrow(() -> new ResourceNotFoundException("Train", "number", number));
        return toDto(train);
    }

    @Transactional(readOnly = true)
    public Page<TrainResponseDto> getAllTrains(int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        return trainRepository.findAll(PageRequest.of(page, safeSize)).map(this::toDto);
    }

    // ✅ NEW: Fare preview — booking se pehle check karo
    // GET /api/trains/{id}/fare?from=NDLS&to=BCT&coach=AC_3
    @Transactional(readOnly = true)
    public FareInfoResponseDto getFarePreview(Long trainId, String fromCode,
                                               String toCode,
                                               com.chirag.train_management_system.enums.CoachType coachType) {
        Train train = findById(trainId);

        Route fromRoute = train.getRoutes().stream()
                .filter(r -> r.getStation().getCode().equalsIgnoreCase(fromCode))
                .findFirst()
                .orElseThrow(() -> new InvalidRouteException(
                        "Station '" + fromCode + "' not on this route."));

        Route toRoute = train.getRoutes().stream()
                .filter(r -> r.getStation().getCode().equalsIgnoreCase(toCode))
                .findFirst()
                .orElseThrow(() -> new InvalidRouteException(
                        "Station '" + toCode + "' not on this route."));

        if (fromRoute.getStopOrder() >= toRoute.getStopOrder()) {
            throw new InvalidRouteException("Invalid direction: FROM must be before TO.");
        }

        double distanceKm = toRoute.getDistanceFromOrigin() - fromRoute.getDistanceFromOrigin();
        java.math.BigDecimal fare = fareCalculator.calculateFare(coachType, train.getTrainType(), distanceKm);

        FareInfoResponseDto dto = new FareInfoResponseDto();
        dto.setFromStation(fromRoute.getStation().getName());
        dto.setToStation(toRoute.getStation().getName());
        dto.setDistanceKm(distanceKm);
        dto.setCoachType(coachType);
        dto.setTrainType(train.getTrainType());
        dto.setBaseFare(coachType.getBaseFare());
        dto.setRatePerKm(coachType.getRatePerKm());
        dto.setTrainTypeMultiplier(train.getTrainType().getPriceMultiplier());
        dto.setFarePerPassenger(fare);
        dto.setBreakdown(String.format(
                "₹%.2f = (₹%s base + ₹%s × %.1f km) × %.2f multiplier",
                fare, coachType.getBaseFare(), coachType.getRatePerKm(),
                distanceKm, train.getTrainType().getPriceMultiplier()));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<TrainResponseDto> searchTrains(String fromInput, String toInput, LocalDate travelDate) {
        if (travelDate == null) {
            throw new BookingValidationException("Travel date is required for searching trains.");
        }
        if (travelDate.isBefore(LocalDate.now())) {
            throw new BookingValidationException("Travel date cannot be in the past.");
        }

        // ✅ Smart resolve: code ya name dono accept karo
        Station fromStation = resolveStation(fromInput);
        Station toStation   = resolveStation(toInput);

        if (fromStation.getId().equals(toStation.getId())) {
            throw new InvalidRouteException("From and To stations cannot be the same.");
        }

        DayOfWeek travelDay   = travelDate.getDayOfWeek();
        boolean   isToday     = travelDate.isEqual(LocalDate.now());
        LocalTime currentTime = LocalTime.now();

        List<Train> candidates = trainRepository.findTrainsBetweenStations(
                fromStation.getId(), toStation.getId(), TrainStatus.ACTIVE);

        return candidates.stream()
                .filter(t -> isValidDirection(t, fromStation.getId(), toStation.getId()))
                .filter(t -> t.getRunningDays() == null
                        || t.getRunningDays().isEmpty()
                        || t.getRunningDays().contains(travelDay))
                .filter(t -> {
                    if (!isToday) return true;
                    return t.getRoutes().stream()
                            .filter(r -> r.getStation().getId().equals(fromStation.getId()))
                            .findFirst()
                            .map(r -> r.getDepartureTime() != null
                                    && r.getDepartureTime().isAfter(currentTime))
                            .orElse(false);
                })
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TrainResponseDto updateTrain(Long id, TrainRequestDto dto) {
        Train train = trainRepository.findByIdWithRoutes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train", "id", String.valueOf(id)));

        long count = bookingRepository.countFutureActiveBookingsByTrainId(id, LocalDate.now());
        if (count > 0) {
            throw new BookingValidationException(
                    "Cannot update train with " + count + " upcoming bookings.");
        }
        if (!train.getTrainNumber().equals(dto.getTrainNumber())
                && trainRepository.existsByTrainNumber(dto.getTrainNumber())) {
            throw new DuplicateResourceException("Train", "number", dto.getTrainNumber());
        }

        validateRoute(dto.getRoutes());
        train.getRoutes().clear();
        trainRepository.saveAndFlush(train);

        train.setTrainName(dto.getTrainName());
        train.setTrainNumber(dto.getTrainNumber());
        train.setRunningDays(dto.getRunningDays());
        train.setTrainType(dto.getTrainType());

        List<Route> routes = buildRoutes(dto.getRoutes(), train);
        train.getRoutes().addAll(routes);

        Train updatedTrain = trainRepository.save(train);
        log.info("Train updated | ID: {} | Number: {}", id, updatedTrain.getTrainNumber());
        return toDto(updatedTrain);
    }

    @Transactional
    public void deleteTrain(Long id) {
        Train train = trainRepository.findByIdWithRoutes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train", "id", String.valueOf(id)));

        long count = bookingRepository.countFutureActiveBookingsByTrainId(id, LocalDate.now());
        if (count > 0) {
            throw new BookingValidationException(
                    "Cannot delete train with " + count + " upcoming booking(s).");
        }
        trainRepository.delete(train);
    }

    private Train findById(Long id) {
        return trainRepository.findByIdWithRoutes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train", "id", String.valueOf(id)));
    }

    private void validateRoute(List<RouteRequestDto> routes) {
        if (routes == null || routes.size() < MIN_STOPS) {
            throw new InvalidRouteException("Minimum 2 stops required");
        }
        long stationCount = routes.stream().map(RouteRequestDto::getStationId).distinct().count();
        if (stationCount != routes.size()) {
            throw new InvalidRouteException("Duplicate stations not allowed");
        }
        long orderCount = routes.stream().map(RouteRequestDto::getStopOrder).distinct().count();
        if (orderCount != routes.size()) {
            throw new InvalidRouteException("Duplicate stop order not allowed");
        }
        int min = routes.stream().mapToInt(RouteRequestDto::getStopOrder).min().orElse(0);
        if (min != 1) {
            throw new InvalidRouteException("Stop order must start from 1");
        }
        for (RouteRequestDto r : routes) {
            if (r.getArrivalTime() == null || r.getDepartureTime() == null) {
                throw new InvalidRouteException("Arrival & Departure time required");
            }
        }
    }

    private List<Route> buildRoutes(List<RouteRequestDto> dtos, Train train) {
        List<Route> list = new ArrayList<>();
        for (RouteRequestDto r : dtos) {
            Station station = stationRepository.findById(r.getStationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Station", "id", String.valueOf(r.getStationId())));
            Route route = new Route();
            route.setTrain(train);
            route.setStation(station);
            route.setStopOrder(r.getStopOrder());
            route.setArrivalTime(r.getArrivalTime());
            route.setDepartureTime(r.getDepartureTime());
            route.setDistanceFromOrigin(r.getDistanceFromOrigin());
            list.add(route);
        }
        return list;
    }

    private boolean isValidDirection(Train train, Long from, Long to) {
        int fromOrder = -1;
        int toOrder   = -1;
        for (Route r : train.getRoutes()) {
            if (r.getStation().getId().equals(from)) fromOrder = r.getStopOrder();
            if (r.getStation().getId().equals(to))   toOrder   = r.getStopOrder();
        }
        return fromOrder != -1 && toOrder != -1 && fromOrder < toOrder;
    }


    // ── Station resolve — code ya name dono se dhundho ───────────────────────
    // Input: "NDLS" ya "New Delhi" ya "new delhi" — dono kaam karenge
    private Station resolveStation(String input) {
        if (input == null || input.isBlank())
            throw new BookingValidationException("Station input cannot be empty.");

        String trimmed = input.trim();

        // 1. Try as code first (short input ya all-caps)
        var byCode = stationRepository.findByCode(trimmed.toUpperCase());
        if (byCode.isPresent()) return byCode.get();

        // 2. Try exact name match (case-insensitive)
        var byName = stationRepository.findByNameContainingIgnoreCase(trimmed);
        if (!byName.isEmpty()) return byName.get(0); // first match

        // 3. Try city match
        var byCity = stationRepository.findByCityIgnoreCase(trimmed);
        if (!byCity.isEmpty()) return byCity.get(0);

        throw new com.chirag.train_management_system.exception.ResourceNotFoundException(
                "Station", "name/code", trimmed);
    }

    private TrainResponseDto toDto(Train train) {
        TrainResponseDto dto = new TrainResponseDto();
        dto.setId(train.getId());
        dto.setTrainName(train.getTrainName());
        dto.setTrainNumber(train.getTrainNumber());
        dto.setStatus(train.getStatus());
        dto.setTrainType(train.getTrainType());
        dto.setRunningDays(train.getRunningDays());

        if (train.getRoutes() != null && !train.getRoutes().isEmpty()) {
            dto.setRoutes(train.getRoutes().stream().map(r -> {
                RouteResponseDto d = new RouteResponseDto();
                d.setId(r.getId());
                d.setStopOrder(r.getStopOrder());
                d.setStationName(r.getStation().getName());
                d.setStationCode(r.getStation().getCode());
                d.setArrivalTime(r.getArrivalTime());
                d.setDepartureTime(r.getDepartureTime());
                d.setDistanceFromOrigin(r.getDistanceFromOrigin());
                return d;
            }).toList());
        }
        return dto;
    }
}