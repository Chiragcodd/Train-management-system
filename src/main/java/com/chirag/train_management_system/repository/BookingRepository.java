package com.chirag.train_management_system.repository;

import com.chirag.train_management_system.entity.Booking;
import com.chirag.train_management_system.entity.Passenger;
import com.chirag.train_management_system.enums.BookingStatus;
import com.chirag.train_management_system.enums.CoachType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    Optional<Booking> findByPnrNumber(String pnrNumber);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.train.id   = :trainId
          AND b.travelDate >= :fromDate
          AND b.status NOT IN ('CANCELLED', 'EXPIRED')
    """)
    long countFutureActiveBookingsByTrainId(
            @Param("trainId")  Long trainId,
            @Param("fromDate") LocalDate fromDate);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.train.id   = :trainId
          AND b.travelDate >= :fromDate
          AND b.status NOT IN ('CANCELLED', 'EXPIRED')
    """)
    long countUpcomingBookingsByTrainId(
            @Param("trainId")  Long trainId,
            @Param("fromDate") LocalDate fromDate);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'PENDING_PAYMENT'
          AND b.paymentExpiresAt < :now
    """)
    List<Booking> findExpiredPendingBookings(@Param("now") LocalDateTime now);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.user.id = :userId
          AND b.travelDate >= :date
          AND b.status NOT IN ('CANCELLED', 'EXPIRED')
    """)
    long countActiveBookingsByUserId(
            @Param("userId") Long userId,
            @Param("date")   LocalDate date);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.train.id   = :trainId
          AND b.coachType  = :coachType
          AND b.travelDate = :travelDate
          AND b.status     = 'WAITLISTED'
    """)
    long countWaitlistedBookings(
            @Param("trainId")    Long trainId,
            @Param("coachType")  CoachType coachType,
            @Param("travelDate") LocalDate travelDate);

    @Query("""
        SELECT COUNT(p) FROM Passenger p
        WHERE p.booking.train.id   = :trainId
          AND p.booking.coachType  = :coachType
          AND p.booking.travelDate = :travelDate
          AND p.passengerStatus    = 'WAITLISTED'
          AND p.booking.status NOT IN ('CANCELLED', 'EXPIRED')
    """)
    long countWaitlistedPassengers(
            @Param("trainId")    Long trainId,
            @Param("coachType")  CoachType coachType,
            @Param("travelDate") LocalDate travelDate);

    @Query("""
        SELECT p FROM Passenger p
        WHERE p.booking.train.id   = :trainId
          AND p.booking.coachType  = :coachType
          AND p.booking.travelDate = :travelDate
          AND p.passengerStatus    = 'WAITLISTED'
          AND p.booking.pnrNumber  IS NOT NULL
          AND p.booking.status NOT IN ('CANCELLED', 'EXPIRED')
        ORDER BY p.waitlistPosition ASC
    """)
    List<Passenger> findPaidWaitlistedPassengersOrdered(
            @Param("trainId")    Long trainId,
            @Param("coachType")  CoachType coachType,
            @Param("travelDate") LocalDate travelDate);
}