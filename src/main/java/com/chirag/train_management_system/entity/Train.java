package com.chirag.train_management_system.entity;

import com.chirag.train_management_system.enums.TrainStatus;
import com.chirag.train_management_system.enums.TrainType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "trains")
@Getter
@Setter
@NoArgsConstructor
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "train_name", nullable = false)
    private String trainName;

    @Column(name = "train_number", nullable = false, unique = true)
    private String trainNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainStatus status = TrainStatus.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "train_running_days",
        joinColumns = @JoinColumn(name = "train_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private Set<DayOfWeek> runningDays = EnumSet.noneOf(DayOfWeek.class);

    @Enumerated(EnumType.STRING)
    @Column(name = "train_type", nullable = false)
    private TrainType trainType = TrainType.EXPRESS;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<Route> routes = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "train")
    private List<Seat> seats = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "train")
    private List<Booking> bookings = new ArrayList<>();
}