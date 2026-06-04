package com.chirag.train_management_system.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotBlank(message = "Payment method is required")
    @Pattern(
        regexp = "^(UPI|CARD|NETBANKING|WALLET)$",
        message = "Payment method must be UPI, CARD, NETBANKING, or WALLET"
    )
    private String paymentMethod;

    // ✅ NEW: Amount — service validate karti hai ki ye booking amount se match kare
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
}