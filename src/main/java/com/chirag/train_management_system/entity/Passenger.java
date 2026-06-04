package com.chirag.train_management_system.entity;

import com.chirag.train_management_system.enums.PassengerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "passengers")
@Getter
@Setter
@NoArgsConstructor
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private String gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * CONFIRMED passengers ka seat assigned hota hai.
     * WAITLISTED passengers ka seat = null — milega jab koi CONFIRMED cancel kare.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "passenger_status", nullable = false)
    private PassengerStatus passengerStatus = PassengerStatus.CONFIRMED;

    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    @Column(name = "fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal fare = BigDecimal.ZERO;
}