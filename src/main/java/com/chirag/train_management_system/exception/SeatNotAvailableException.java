package com.chirag.train_management_system.exception;

public class SeatNotAvailableException extends RuntimeException {

    public SeatNotAvailableException(String coachType, int requested, int available) {
        super("Not enough seats in " + coachType + " coach. " +
              "Requested: " + requested + ", Available: " + available);
    }
}