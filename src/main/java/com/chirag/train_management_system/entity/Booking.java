// package com.chirag.train_management_system.entity;

// import com.chirag.train_management_system.enums.BookingStatus;
// import com.chirag.train_management_system.enums.CoachType;
// import jakarta.persistence.*;
// import lombok.*;

// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;

// @Entity
// @Table(name = "bookings")
// @Getter
// @Setter
// @NoArgsConstructor
// public class Booking {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long bookingId;

//     // PNR payment ke baad generate hoga — tab tak null
//     @Column(name = "pnr_number", nullable = true, unique = true)
//     private String pnrNumber;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "user_id", nullable = false)
//     private User user;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "train_id", nullable = false)
//     private Train train;

//     @ManyToOne(fetch = FetchType.EAGER)
//     @JoinColumn(name = "from_station_id", nullable = false)
//     private Station fromStation;

//     @ManyToOne(fetch = FetchType.EAGER)
//     @JoinColumn(name = "to_station_id", nullable = false)
//     private Station toStation;

//     @Enumerated(EnumType.STRING)
//     @Column(name = "coach_type", nullable = false)
//     private CoachType coachType;

//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false)
//     private BookingStatus status;

//     @Column(name = "travel_date", nullable = false)
//     private LocalDate travelDate;

//     @Column(name = "booking_date", nullable = false)
//     private LocalDateTime bookingDate;

//     @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
//     private BigDecimal totalAmount;

//     @Column(name = "refund_amount", precision = 10, scale = 2)
//     private BigDecimal refundAmount = BigDecimal.ZERO;

//     @Column(name = "payment_expires_at")
//     private LocalDateTime paymentExpiresAt;

//     // Dynamic fare ke liye distance store karo booking time pe
//     @Column(name = "journey_distance_km", nullable = false)
//     private double journeyDistanceKm = 0.0;

//     // Waitlist position — WL/1, WL/2 etc. — null if not waitlisted
//     @Column(name = "waitlist_number")
//     private Integer waitlistNumber;

//     @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
//     private List<Passenger> passengers = new ArrayList<>();

//     @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//     private Payment payment;
// }



package com.chirag.train_management_system.entity;

import com.chirag.train_management_system.enums.BookingStatus;
import com.chirag.train_management_system.enums.CoachType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    // PNR payment ke baad generate hoga — tab tak null
    @Column(name = "pnr_number", nullable = true, unique = true)
    private String pnrNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_station_id", nullable = false)
    private Station fromStation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_station_id", nullable = false)
    private Station toStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "coach_type", nullable = false)
    private CoachType coachType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "payment_expires_at")
    private LocalDateTime paymentExpiresAt;

    // Dynamic fare ke liye distance store karo booking time pe
    @Column(name = "journey_distance_km", nullable = false)
    private double journeyDistanceKm = 0.0;

    // Waitlist position — WL/1, WL/2 etc. — null if not waitlisted
    @Column(name = "waitlist_number")
    private Integer waitlistNumber;

    // Cancellation timestamp
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Passenger> passengers = new ArrayList<>();

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;
}