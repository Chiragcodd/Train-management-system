// package com.chirag.train_management_system.service;

// import com.chirag.train_management_system.dto.*;
// import com.chirag.train_management_system.entity.*;
// import com.chirag.train_management_system.exception.*;
// import com.chirag.train_management_system.repository.*;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.util.ArrayList;
// import java.util.List;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class SeatService {

//     private static final int MAX_SEATS_PER_COACH = 500;

//     private final SeatRepository  seatRepository;
//     private final TrainRepository trainRepository;

//     @Transactional
//     public List<SeatResponseDto> addSeats(SeatRequestDto dto) {
//         Train train = findTrainById(dto.getTrainId());
//         List<SeatResponseDto> added = new ArrayList<>();

//         for (CoachRequestDto coach : dto.getCoaches()) {
//             if (coach.getCount() <= 0)
//                 throw new BookingValidationException(
//                         "Seat count must be > 0 for: " + coach.getCoachType());

//             long coachBookings = seatRepository.countUpcomingBookingsByCoachType(
//                     train.getId(), coach.getCoachType(), LocalDate.now());
//             if (coachBookings > 0)
//                 throw new BookingValidationException(
//                         "Cannot modify " + coach.getCoachType()
//                         + ". It has " + coachBookings + " upcoming booking(s).");

//             int existing = seatRepository.countByTrainIdAndCoachType(
//                     train.getId(), coach.getCoachType());
//             if (existing + coach.getCount() > MAX_SEATS_PER_COACH)
//                 throw new BookingValidationException(
//                         "Cannot add " + coach.getCount() + " seats. "
//                         + coach.getCoachType() + " already has " + existing
//                         + ". Max: " + MAX_SEATS_PER_COACH);

//             // price = 0 — actual fare dynamically calculated at booking time
//             List<Seat> toSave = new ArrayList<>();
//             for (int i = 1; i <= coach.getCount(); i++) {
//                 Seat s = new Seat();
//                 s.setTrain(train);
//                 s.setCoachType(coach.getCoachType());
//                 s.setSeatNumber(existing + i);
//                 s.setPrice(BigDecimal.ZERO);
//                 s.setBooked(false);
//                 toSave.add(s);
//             }

//             seatRepository.saveAll(toSave).forEach(s -> added.add(toDto(s)));

//             log.info("Seats added | train={} | coach={} | count={} | rate=₹{}/km",
//                     train.getId(), coach.getCoachType(), coach.getCount(),
//                     coach.getCoachType().getRatePerKm());
//         }

//         return added;
//     }

//     @Transactional(readOnly = true)
//     public SeatResponseDto getSeatById(Long id) {
//         return toDto(findSeatById(id));
//     }

//     @Transactional(readOnly = true)
//     public List<SeatResponseDto> getSeatsByTrain(Long trainId) {
//         findTrainById(trainId);
//         return seatRepository.findByTrainIdOrderBySeatNumber(trainId)
//                 .stream().map(this::toDto).toList();
//     }

//     @Transactional(readOnly = true)
//     public List<SeatResponseDto> getAvailableSeats(Long trainId, LocalDate travelDate) {
//         findTrainById(trainId);
//         if (travelDate == null)
//             throw new BookingValidationException("Travel date is required.");
//         if (travelDate.isBefore(LocalDate.now()))
//             throw new BookingValidationException("Travel date cannot be in the past.");
//         return seatRepository.findAvailableSeatsByDate(trainId, travelDate)
//                 .stream().map(this::toDto).toList();
//     }

//     @Transactional
//     public void deleteSeat(Long id) {
//         Seat seat = findSeatById(id);
//         long upcoming = seatRepository.countUpcomingBookingsBySeatId(id, LocalDate.now());
//         if (upcoming > 0)
//             throw new BookingValidationException(
//                     "Cannot delete seat #" + seat.getSeatNumber()
//                     + " (" + seat.getCoachType() + "). "
//                     + upcoming + " upcoming booking(s).");
//         seatRepository.deleteById(id);
//         log.info("Seat deleted | id={} | coach={} | seat#={}",
//                 id, seat.getCoachType(), seat.getSeatNumber());
//     }

//     private Seat findSeatById(Long id) {
//         return seatRepository.findById(id)
//                 .orElseThrow(() -> new ResourceNotFoundException("Seat", id));
//     }

//     private Train findTrainById(Long id) {
//         return trainRepository.findById(id)
//                 .orElseThrow(() -> new ResourceNotFoundException("Train", id));
//     }

//     private SeatResponseDto toDto(Seat s) {
//         SeatResponseDto dto = new SeatResponseDto();
//         dto.setId(s.getId());
//         dto.setSeatNumber(s.getSeatNumber());
//         dto.setCoachType(s.getCoachType());
//         dto.setTrainId(s.getTrain().getId());
//         dto.setTrainName(s.getTrain().getTrainName());
//         dto.setRatePerKm(s.getCoachType().getRatePerKm());
//         dto.setBaseFare(s.getCoachType().getBaseFare());
//         return dto;
//     }
// }







package com.chirag.train_management_system.service;

import com.chirag.train_management_system.dto.*;
import com.chirag.train_management_system.entity.*;
import com.chirag.train_management_system.exception.*;
import com.chirag.train_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chirag.train_management_system.enums.CoachType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
// import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private static final int MAX_SEATS_PER_COACH = 500;

    private final SeatRepository  seatRepository;
    private final TrainRepository trainRepository;

    @Transactional
    public List<SeatResponseDto> addSeats(SeatRequestDto dto) {
        Train train = findTrainById(dto.getTrainId());
        List<SeatResponseDto> added = new ArrayList<>();

        for (CoachRequestDto coach : dto.getCoaches()) {
            if (coach.getCount() <= 0)
                throw new BookingValidationException(
                        "Seat count must be > 0 for: " + coach.getCoachType());

            long coachBookings = seatRepository.countUpcomingBookingsByCoachType(
                    train.getId(), coach.getCoachType(), LocalDate.now());
            if (coachBookings > 0)
                throw new BookingValidationException(
                        "Cannot modify " + coach.getCoachType()
                        + ". It has " + coachBookings + " upcoming booking(s).");

            int existing = seatRepository.countByTrainIdAndCoachType(
                    train.getId(), coach.getCoachType());
            if (existing + coach.getCount() > MAX_SEATS_PER_COACH)
                throw new BookingValidationException(
                        "Cannot add " + coach.getCount() + " seats. "
                        + coach.getCoachType() + " already has " + existing
                        + ". Max: " + MAX_SEATS_PER_COACH);

            // price = 0 — actual fare dynamically calculated at booking time
            List<Seat> toSave = new ArrayList<>();
            for (int i = 1; i <= coach.getCount(); i++) {
                Seat s = new Seat();
                s.setTrain(train);
                s.setCoachType(coach.getCoachType());
                s.setSeatNumber(existing + i);
                s.setPrice(BigDecimal.ZERO);
                s.setBooked(false);
                toSave.add(s);
            }

            seatRepository.saveAll(toSave).forEach(s -> added.add(toDto(s)));

            log.info("Seats added | train={} | coach={} | count={} | rate=₹{}/km",
                    train.getId(), coach.getCoachType(), coach.getCount(),
                    coach.getCoachType().getRatePerKm());
        }

        return added;
    }

    @Transactional(readOnly = true)
    public SeatResponseDto getSeatById(Long id) {
        return toDto(findSeatById(id));
    }

    @Transactional(readOnly = true)
    public List<SeatResponseDto> getSeatsByTrain(Long trainId) {
        findTrainById(trainId);
        return seatRepository.findByTrainIdOrderBySeatNumber(trainId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponseDto> getAvailableSeats(Long trainId, LocalDate travelDate) {
        findTrainById(trainId);
        if (travelDate == null)
            throw new BookingValidationException("Travel date is required.");
        if (travelDate.isBefore(LocalDate.now()))
            throw new BookingValidationException("Travel date cannot be in the past.");
        return seatRepository.findAvailableSeatsByDate(trainId, travelDate)
                .stream().map(this::toDto).toList();
    }

    // ✅ NEW: Coach-wise seat availability for a train on a specific travel date
    // Search result mein "Check Availability" button ke liye
    @Transactional(readOnly = true)
    public TrainSeatAvailabilityDto getTrainSeatAvailability(Long trainId, LocalDate travelDate) {
        Train train = findTrainById(trainId);

        if (travelDate == null)
            throw new BookingValidationException("Travel date is required.");
        if (travelDate.isBefore(LocalDate.now()))
            throw new BookingValidationException("Travel date cannot be in the past.");

        List<CoachAvailabilityDto> coachList = new ArrayList<>();

        for (CoachType coachType : CoachType.values()) {
            int total     = seatRepository.countByTrainIdAndCoachType(trainId, coachType);
            if (total == 0) continue; // is train mein ye coach nahi hai

            int available    = seatRepository.countAvailableByCoachTypeAndDate(trainId, coachType, travelDate);
            int booked       = total - available;
            int waitlisted   = seatRepository.countWaitlistedByCoachTypeAndDate(trainId, coachType, travelDate);

            coachList.add(new CoachAvailabilityDto(
                    coachType,
                    total,
                    available,
                    booked,
                    waitlisted,
                    coachType.getBaseFare(),
                    coachType.getRatePerKm()
            ));
        }

        TrainSeatAvailabilityDto dto = new TrainSeatAvailabilityDto();
        dto.setTrainId(train.getId());
        dto.setTrainName(train.getTrainName());
        dto.setTrainNumber(train.getTrainNumber());
        dto.setTravelDate(travelDate);
        dto.setCoaches(coachList);
        return dto;
    }

    @Transactional
    public void deleteSeat(Long id) {
        Seat seat = findSeatById(id);
        long upcoming = seatRepository.countUpcomingBookingsBySeatId(id, LocalDate.now());
        if (upcoming > 0)
            throw new BookingValidationException(
                    "Cannot delete seat #" + seat.getSeatNumber()
                    + " (" + seat.getCoachType() + "). "
                    + upcoming + " upcoming booking(s).");
        seatRepository.deleteById(id);
        log.info("Seat deleted | id={} | coach={} | seat#={}",
                id, seat.getCoachType(), seat.getSeatNumber());
    }

    private Seat findSeatById(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", id));
    }

    private Train findTrainById(Long id) {
        return trainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train", id));
    }

    private SeatResponseDto toDto(Seat s) {
        SeatResponseDto dto = new SeatResponseDto();
        dto.setId(s.getId());
        dto.setSeatNumber(s.getSeatNumber());
        dto.setCoachType(s.getCoachType());
        dto.setTrainId(s.getTrain().getId());
        dto.setTrainName(s.getTrain().getTrainName());
        dto.setRatePerKm(s.getCoachType().getRatePerKm());
        dto.setBaseFare(s.getCoachType().getBaseFare());
        return dto;
    }
}