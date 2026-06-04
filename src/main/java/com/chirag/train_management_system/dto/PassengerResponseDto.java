// package com.chirag.train_management_system.dto;

// import lombok.Data;
// import java.math.BigDecimal;

// @Data
// public class PassengerResponseDto {
//     private Long id;
//     private String name;
//     private int age;
//     private String gender;
//     private int seatNumber;
//     private String coachType;
//     private BigDecimal fare;
// }




package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.PassengerStatus;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PassengerResponseDto {
    private Long   id;
    private String name;
    private int    age;
    private String gender;

    /**
     * Seat number:
     *   - CONFIRMED passengers → actual seat number (e.g. 7)
     *   - WAITLISTED passengers → 0 (seat nahi mili abhi tak)
     */
    private int    seatNumber;
    private String coachType;

    /**
     * Per-passenger status — CONFIRMED ya WAITLISTED.
     * Ek booking mein mixed ho sakta hai.
     */
    private PassengerStatus passengerStatus;

    /**
     * WL position — WL/1, WL/2, etc.
     * null agar CONFIRMED.
     */
    private Integer waitlistPosition;

    private BigDecimal fare;
}