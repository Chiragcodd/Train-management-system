package com.chirag.train_management_system.entity;

import com.chirag.train_management_system.enums.CoachType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"train_id", "coach_type", "seat_number"},
        name = "uk_train_coach_seat"
    )
)
@Getter
@Setter
@NoArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Enumerated(EnumType.STRING)
    @Column(name = "coach_type", nullable = false)
    private CoachType coachType;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    // Legacy field — always 0.00. Actual fare Passenger.fare mein hai.
    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean booked = false;

    @Version
    private Long version;
}