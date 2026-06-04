package com.chirag.train_management_system.service;

import com.chirag.train_management_system.dto.*;
import com.chirag.train_management_system.entity.*;
import com.chirag.train_management_system.enums.BookingStatus;
import com.chirag.train_management_system.enums.PassengerStatus;
import com.chirag.train_management_system.enums.TrainStatus;
import com.chirag.train_management_system.exception.*;
import com.chirag.train_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int MAX_PAGE_SIZE        = 100;
    private static final int MAX_PASSENGERS       = 6;
    private static final int MAX_ADVANCE_DAYS     = 120;
    private static final int BOOKING_CUTOFF_HOURS = 4;
    private static final int PAYMENT_WINDOW_MINS  = 5;

    private final BookingRepository     bookingRepository;
    private final UserRepository        userRepository;
    private final TrainRepository       trainRepository;
    private final SeatRepository        seatRepository;
    private final RouteRepository       routeRepository;
    private final FareCalculatorService fareCalculator;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BookingResponseDto bookTicket(
            BookingRequestDto dto, Authentication authentication) {

        // 1. User load
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "username", authentication.getName()));

        // 2. Train load
        Train train = trainRepository.findById(dto.getTrainId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Train", dto.getTrainId()));

        if (train.getStatus() != TrainStatus.ACTIVE)
            throw new InvalidRouteException("Train is not active.");

        // 3. Passenger count
        if (dto.getPassengers() == null || dto.getPassengers().isEmpty())
            throw new BookingValidationException("At least 1 passenger is required.");
        if (dto.getPassengers().size() > MAX_PASSENGERS)
            throw new BookingValidationException(
                    "Maximum " + MAX_PASSENGERS + " passengers allowed per booking.");

        // 4. Travel date
        LocalDate today      = LocalDate.now();
        LocalDate travelDate = dto.getTravelDate();
        if (travelDate.isBefore(today))
            throw new BookingValidationException("Travel date cannot be in the past.");
        if (travelDate.isAfter(today.plusDays(MAX_ADVANCE_DAYS)))
            throw new BookingValidationException(
                    "Booking allowed only up to " + MAX_ADVANCE_DAYS + " days in advance.");

        // 5. Running days
        if (train.getRunningDays() != null && !train.getRunningDays().isEmpty()) {
            DayOfWeek travelDay = travelDate.getDayOfWeek();
            if (!train.getRunningDays().contains(travelDay))
                throw new InvalidRouteException(
                        "Train " + train.getTrainNumber() + " does not run on "
                        + travelDay + ". Runs on: " + train.getRunningDays());
        }

        // 6. Route validation
        Route fromRoute = routeRepository
                .findByTrainIdAndStationCode(train.getId(),
                        dto.getFromStationCode().toUpperCase())
                .orElseThrow(() -> new InvalidRouteException(
                        "Station '" + dto.getFromStationCode()
                        + "' is not on this train's route."));

        Route toRoute = routeRepository
                .findByTrainIdAndStationCode(train.getId(),
                        dto.getToStationCode().toUpperCase())
                .orElseThrow(() -> new InvalidRouteException(
                        "Station '" + dto.getToStationCode()
                        + "' is not on this train's route."));

        if (fromRoute.getStopOrder() >= toRoute.getStopOrder())
            throw new InvalidRouteException(
                    "Invalid direction: FROM station must come before TO station.");

        // 7. Booking cutoff
        LocalDateTime departureDT = LocalDateTime.of(
                travelDate, fromRoute.getDepartureTime());
        if (LocalDateTime.now().isAfter(departureDT.minusHours(BOOKING_CUTOFF_HOURS)))
            throw new BookingValidationException(
                    "Booking closed. Cannot book within " + BOOKING_CUTOFF_HOURS
                    + " hours of departure from "
                    + fromRoute.getStation().getName() + ".");

        // 8. Passenger validation
        validatePassengers(dto.getPassengers());

        // 9. Fare calculation
        double journeyDistanceKm =
                toRoute.getDistanceFromOrigin() - fromRoute.getDistanceFromOrigin();
        if (journeyDistanceKm <= 0)
            throw new BookingValidationException("Invalid route distance.");

        BigDecimal farePerPassenger = fareCalculator.calculateFare(
                dto.getCoachType(), train.getTrainType(), journeyDistanceKm);

        // 10. Available seats (PESSIMISTIC_WRITE lock)
        List<Seat> availableSeats = seatRepository.findAvailableSeatsForDate(
                train.getId(), dto.getCoachType(), travelDate);

        // 11. Validate ki is coachType ki seats exist karti hain
        if (availableSeats.isEmpty()
                && !seatRepository.existsByTrainIdAndCoachType(
                        train.getId(), dto.getCoachType()))
            throw new BookingValidationException(
                    "No " + dto.getCoachType() + " coach configured in this train.");

        int passengerCount  = dto.getPassengers().size();
        int availableCount  = availableSeats.size();

        long currentWlCount = bookingRepository.countWaitlistedPassengers(
                train.getId(), dto.getCoachType(), travelDate);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrain(train);
        booking.setFromStation(fromRoute.getStation());
        booking.setToStation(toRoute.getStation());
        booking.setCoachType(dto.getCoachType());
        booking.setTravelDate(travelDate);
        booking.setBookingDate(LocalDateTime.now());
        booking.setJourneyDistanceKm(journeyDistanceKm);
        booking.setPaymentExpiresAt(
                LocalDateTime.now().plusMinutes(PAYMENT_WINDOW_MINS));

        List<Passenger> passengers  = booking.getPassengers();
        BigDecimal      totalAmount = BigDecimal.ZERO;
        int             cnfCount    = 0;
        int             wlCount     = 0;

        for (int i = 0; i < passengerCount; i++) {
            PassengerRequestDto pReq = dto.getPassengers().get(i);

            Passenger p = new Passenger();
            p.setName(pReq.getName());
            p.setAge(pReq.getAge());
            p.setGender(pReq.getGender());
            p.setFare(farePerPassenger);
            p.setBooking(booking);

            if (i < availableCount) {
                // ── CONFIRMED: seat available hai ──────────────────────────
                p.setSeat(availableSeats.get(i));
                p.setPassengerStatus(PassengerStatus.CONFIRMED);
                p.setWaitlistPosition(null);
                cnfCount++;

            } else {
                // ── WAITLISTED: seat nahi bachi ────────────────────────────
                p.setSeat(null);
                p.setPassengerStatus(PassengerStatus.WAITLISTED);
                // Global WL position — baaki WL ke saath continuous sequence
                p.setWaitlistPosition((int) currentWlCount + (wlCount + 1));
                wlCount++;
            }

            passengers.add(p);
            totalAmount = totalAmount.add(farePerPassenger);
        }

        if (cnfCount > 0) {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
            booking.setWaitlistNumber(null);
        } else {
            booking.setStatus(BookingStatus.WAITLISTED);
            // Booking-level WL number — first WL passenger ki position
            booking.setWaitlistNumber(
                    passengers.get(0).getWaitlistPosition());
        }

        booking.setPassengers(passengers);
        booking.setTotalAmount(totalAmount);

        Booking saved = bookingRepository.save(booking);

        log.info("Booking created | Id={} | User={} | Train={} | Date={} "
                + "| Status={} | CNF={} | WL={} | FarePerPax=₹{} | Total=₹{}",
                saved.getBookingId(), user.getUsername(),
                train.getTrainNumber(), travelDate,
                saved.getStatus(), cnfCount, wlCount,
                farePerPassenger, totalAmount);

        return toDto(saved);
    }

    @Transactional
    public BookingResponseDto cancelBooking(
            Long bookingId, Authentication authentication) {

        Booking booking = findById(bookingId);
        checkOwnershipOrAdmin(booking.getUser().getUsername(), authentication);

        if (booking.getStatus() == BookingStatus.CANCELLED)
            throw new BookingCancellationException("Booking is already cancelled.");
        if (booking.getStatus() == BookingStatus.EXPIRED)
            throw new BookingCancellationException("Booking has already expired.");

        LocalTime fromDep = booking.getTrain().getRoutes().stream()
                .filter(r -> r.getStation().getId()
                        .equals(booking.getFromStation().getId()))
                .map(Route::getDepartureTime)
                .findFirst()
                .orElseThrow(() -> new InvalidRouteException(
                        "From station not found in train route."));

        LocalDateTime departureDT = LocalDateTime.of(
                booking.getTravelDate(), fromDep);

        if (LocalDateTime.now().isAfter(departureDT))
            throw new BookingCancellationException(
                    "Cannot cancel after departure from "
                    + booking.getFromStation().getName() + ".");

        BookingStatus prevStatus = booking.getStatus();
        BigDecimal refund;

        if (prevStatus == BookingStatus.PENDING_PAYMENT) {
            refund = BigDecimal.ZERO;
        } else if (prevStatus == BookingStatus.WAITLISTED) {
            // Paid (pnrNumber set) → full refund; Unpaid → ₹0
            refund = booking.getPnrNumber() != null
                    ? booking.getTotalAmount()
                    : BigDecimal.ZERO;
        } else {
            // CONFIRMED → time-based slab
            refund = calculateRefund(booking, departureDT);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setRefundAmount(refund);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        log.info("Booking cancelled | Id={} | PNR={} | PrevStatus={} | Refund=₹{}",
                booking.getBookingId(), booking.getPnrNumber(), prevStatus, refund);

        // CONFIRMED cancel → seats free → promote WL passengers
        if (prevStatus == BookingStatus.CONFIRMED) {
            promoteWaitlistedPassengers(booking);
        }

        return toDto(booking);
    }

    private void promoteWaitlistedPassengers(Booking cancelledBooking) {

        // Paid WL passengers — ordered by waitlist_position ASC
        List<Passenger> wlPassengers = bookingRepository.findPaidWaitlistedPassengersOrdered(
                cancelledBooking.getTrain().getId(),
                cancelledBooking.getCoachType(),
                cancelledBooking.getTravelDate());

        if (wlPassengers.isEmpty()) return;

        // Seats jo ab available hain
        List<Seat> availableSeats = seatRepository.findAvailableSeatsForDate(
                cancelledBooking.getTrain().getId(),
                cancelledBooking.getCoachType(),
                cancelledBooking.getTravelDate());

        if (availableSeats.isEmpty()) return;

        int promoted = 0;
        int seatIdx  = 0;

        for (Passenger wlP : wlPassengers) {
            if (seatIdx >= availableSeats.size()) break;  // seats khatam

            wlP.setSeat(availableSeats.get(seatIdx++));
            wlP.setPassengerStatus(PassengerStatus.CONFIRMED);
            wlP.setWaitlistPosition(null);
            promoted++;

            // Agar us booking ke SAARE passengers ab CONFIRMED hain
            // → booking status bhi update karo
            Booking wlBooking = wlP.getBooking();
            boolean allConfirmed = wlBooking.getPassengers().stream()
                    .allMatch(p -> p.getPassengerStatus() == PassengerStatus.CONFIRMED);
            if (allConfirmed) {
                wlBooking.setStatus(BookingStatus.CONFIRMED);
                wlBooking.setWaitlistNumber(null);
                bookingRepository.save(wlBooking);
                log.info("All passengers CONFIRMED | BookingId={} | PNR={}",
                        wlBooking.getBookingId(), wlBooking.getPnrNumber());
            }
        }

        log.info("WL promotion done | {} passenger(s) promoted | CancelledBooking={}",
                promoted, cancelledBooking.getBookingId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  READ OPERATIONS
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(Long id, Authentication auth) {
        Booking b = findById(id);
        checkOwnershipOrAdmin(b.getUser().getUsername(), auth);
        return toDto(b);
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getBookingByPnr(String pnr, Authentication auth) {
        Booking b = bookingRepository.findByPnrNumber(pnr.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "PNR", pnr));
        checkOwnershipOrAdmin(b.getUser().getUsername(), auth);
        return toDto(b);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponseDto> getUserBookingsForAdmin(
            Long userId, int page, int size) {
        return bookingRepository.findByUserId(userId,
                PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                        Sort.by("travelDate").descending()))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponseDto> getMyBookings(
            int page, int size, String status, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "username", auth.getName()));
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by("bookingDate").descending());

        if (status != null && !status.equalsIgnoreCase("ALL")) {
            BookingStatus bs = BookingStatus.valueOf(status.toUpperCase());
            return bookingRepository.findByUserIdAndStatus(user.getId(), bs, pageable)
                    .map(this::toDto);
        }
        return bookingRepository.findByUserId(user.getId(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponseDto> getAllBookings(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by("bookingDate").descending());

        if (status != null && !status.equalsIgnoreCase("ALL")) {
            BookingStatus bs = BookingStatus.valueOf(status.toUpperCase());
            return bookingRepository.findByStatus(bs, pageable)
                    .map(this::toDto);
        }
        return bookingRepository.findAll(pageable)
                .map(this::toDto);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void validatePassengers(List<PassengerRequestDto> passengers) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < passengers.size(); i++) {
            PassengerRequestDto p = passengers.get(i);
            if (p.getName() == null || p.getName().isBlank())
                throw new BookingValidationException(
                        "Passenger " + (i+1) + ": name cannot be empty.");
            if (p.getAge() == null || p.getAge() < 1 || p.getAge() > 120)
                throw new BookingValidationException(
                        "Passenger " + (i+1) + ": age must be 1–120.");
            if (p.getGender() == null)
                throw new BookingValidationException(
                        "Passenger " + (i+1) + ": gender is required.");
            String key = p.getName().trim().toLowerCase() + "_" + p.getAge();
            if (!seen.add(key))
                throw new BookingValidationException(
                        "Duplicate passenger: " + p.getName()
                        + " (age " + p.getAge() + ").");
        }
    }

    private BigDecimal calculateRefund(Booking b, LocalDateTime departureDT) {
        long h = Duration.between(LocalDateTime.now(), departureDT).toHours();
        BigDecimal t = b.getTotalAmount();
        if (h >= 48) return t.multiply(new BigDecimal("0.75"));
        if (h >= 12) return t.multiply(new BigDecimal("0.50"));
        if (h >=  4) return t.multiply(new BigDecimal("0.25"));
        return BigDecimal.ZERO;
    }

    private Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private void checkOwnershipOrAdmin(String owner, Authentication auth) {
        boolean isOwner = owner.equals(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin)
            throw new CustomAccessDeniedException("Access denied.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DTO MAPPER
    // ─────────────────────────────────────────────────────────────────────────
    private BookingResponseDto toDto(Booking b) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingId(b.getBookingId());
        dto.setPnrNumber(b.getPnrNumber());
        dto.setUserName(b.getUser().getName());
        dto.setTrainName(b.getTrain().getTrainName());
        dto.setTrainNumber(b.getTrain().getTrainNumber());
        dto.setFromStation(b.getFromStation().getName());
        dto.setToStation(b.getToStation().getName());
        dto.setCoachType(b.getCoachType());
        dto.setTravelDate(b.getTravelDate());
        dto.setBookingDate(b.getBookingDate());
        dto.setStatus(b.getStatus());
        dto.setTotalAmount(b.getTotalAmount());
        dto.setRefundAmount(b.getRefundAmount());
        dto.setPaymentExpiresAt(b.getPaymentExpiresAt());
        dto.setWaitlistNumber(b.getWaitlistNumber());
        dto.setCancelledAt(b.getCancelledAt());
        dto.setJourneyDistanceKm(b.getJourneyDistanceKm());

        List<PassengerResponseDto> pList = b.getPassengers().stream()
                .map(p -> {
                    PassengerResponseDto pr = new PassengerResponseDto();
                    pr.setId(p.getId());
                    pr.setName(p.getName());
                    pr.setAge(p.getAge());
                    pr.setGender(p.getGender());
                    pr.setFare(p.getFare());
                    pr.setPassengerStatus(p.getPassengerStatus());
                    pr.setWaitlistPosition(p.getWaitlistPosition());

                    if (p.getSeat() != null) {
                        pr.setSeatNumber(p.getSeat().getSeatNumber());
                        pr.setCoachType(p.getSeat().getCoachType().name());
                    } else {
                        pr.setSeatNumber(0);
                        pr.setCoachType(b.getCoachType().name());
                    }
                    return pr;
                })
                .toList();

        dto.setPassengers(pList);

        // CNF / WL counts
        long cnf = pList.stream()
                .filter(p -> p.getPassengerStatus() == PassengerStatus.CONFIRMED)
                .count();
        dto.setConfirmedCount((int) cnf);
        dto.setWaitlistedCount(pList.size() - (int) cnf);

        // Fare breakdown
        if (!pList.isEmpty()) {
            BigDecimal farePerPax = pList.get(0).getFare();
            dto.setFarePerPassenger(farePerPax);
            dto.setFareBreakdown(String.format(
                    "%s | %s | %.1f km → ₹%.2f per passenger",
                    b.getCoachType(),
                    b.getTrain().getTrainType(),
                    b.getJourneyDistanceKm(),
                    farePerPax));
        }

        return dto;
    }
}