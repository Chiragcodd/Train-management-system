package com.chirag.train_management_system.repository;

import com.chirag.train_management_system.entity.Seat;
import com.chirag.train_management_system.enums.CoachType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByTrainIdOrderBySeatNumber(Long trainId);

    int countByTrainIdAndCoachType(Long trainId, CoachType coachType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s FROM Seat s
        WHERE s.train.id  = :trainId
          AND s.coachType = :coachType
          AND s.id NOT IN (
              SELECT p.seat.id FROM Passenger p
              WHERE p.booking.travelDate = :travelDate
                AND p.booking.status NOT IN ('CANCELLED', 'EXPIRED')
                AND p.seat IS NOT NULL
          )
        ORDER BY s.seatNumber ASC
    """)
    List<Seat> findAvailableSeatsForDate(
            @Param("trainId")    Long trainId,
            @Param("coachType")  CoachType coachType,
            @Param("travelDate") LocalDate travelDate);

    @Query("""
        SELECT s FROM Seat s
        WHERE s.train.id = :trainId
          AND s.id NOT IN (
              SELECT p.seat.id FROM Passenger p
              WHERE p.booking.travelDate = :travelDate
                AND p.booking.status NOT IN ('CANCELLED', 'EXPIRED')
                AND p.seat IS NOT NULL
          )
        ORDER BY s.seatNumber ASC
    """)
    List<Seat> findAvailableSeatsByDate(
            @Param("trainId")    Long trainId,
            @Param("travelDate") LocalDate travelDate);

    @Query("""
        SELECT COUNT(p) FROM Passenger p
        WHERE p.seat.train.id  = :trainId
          AND p.seat.coachType = :coachType
          AND p.booking.travelDate >= :date
          AND p.booking.status NOT IN ('CANCELLED', 'EXPIRED')
    """)
    long countUpcomingBookingsByCoachType(
            @Param("trainId")   Long trainId,
            @Param("coachType") CoachType coachType,
            @Param("date")      LocalDate date);

    @Query("""
        SELECT COUNT(p) FROM Passenger p
        WHERE p.seat.id = :seatId
          AND p.booking.travelDate >= :date
          AND p.booking.status NOT IN ('CANCELLED', 'EXPIRED')
    """)
    long countUpcomingBookingsBySeatId(
            @Param("seatId") Long seatId,
            @Param("date")   LocalDate date);

    @Query("""
        SELECT COUNT(s) > 0 FROM Seat s
        WHERE s.train.id  = :trainId
          AND s.coachType = :coachType
    """)
    boolean existsByTrainIdAndCoachType(
            @Param("trainId")   Long trainId,
            @Param("coachType") CoachType coachType);

    @Query("""
        SELECT COUNT(s) FROM Seat s
        WHERE s.train.id  = :trainId
          AND s.coachType = :coachType
          AND s.id NOT IN (
              SELECT p.seat.id FROM Passenger p
              WHERE p.booking.travelDate = :travelDate
                AND p.booking.status NOT IN ('CANCELLED', 'EXPIRED')
                AND p.seat IS NOT NULL
          )
    """)
    int countAvailableByCoachTypeAndDate(
            @Param("trainId")    Long trainId,
            @Param("coachType")  CoachType coachType,
            @Param("travelDate") LocalDate travelDate);

    @Query("""
        SELECT COUNT(p) FROM Passenger p
        WHERE p.booking.train.id      = :trainId
          AND p.booking.coachType     = :coachType
          AND p.booking.travelDate    = :travelDate
          AND p.passengerStatus       = com.chirag.train_management_system.enums.PassengerStatus.WAITLISTED
          AND p.booking.status NOT IN ('CANCELLED', 'EXPIRED')
    """)
    int countWaitlistedByCoachTypeAndDate(
            @Param("trainId")    Long trainId,
            @Param("coachType")  CoachType coachType,
            @Param("travelDate") LocalDate travelDate);
}