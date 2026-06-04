package com.chirag.train_management_system.exception;

public class SeatAlreadyBookedException extends RuntimeException {

    public SeatAlreadyBookedException(Long seatId) {
        super("Seat with id " + seatId + " is already booked and cannot be deleted.");
    }
}