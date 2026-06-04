package com.chirag.train_management_system.dto;

import com.chirag.train_management_system.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundResponseDto {
    private Long paymentId;
    private Long bookingId;
    private String pnrNumber;
    private BigDecimal originalAmount;
    private BigDecimal refundAmount;
    private String refundTransactionId;
    private PaymentStatus status;
    private LocalDateTime refundDate;
    private String message;
}