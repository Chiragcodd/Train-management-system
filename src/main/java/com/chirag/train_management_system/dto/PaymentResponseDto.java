package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDto {
    private Long paymentId;
    private Long bookingId;
    private String pnrNumber;
    private String userName;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;
    private PaymentStatus status;
    private LocalDateTime paymentDate;
}