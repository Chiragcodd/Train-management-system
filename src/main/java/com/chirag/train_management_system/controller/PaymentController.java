package com.chirag.train_management_system.controller;

import com.chirag.train_management_system.dto.PaymentRequestDto;
import com.chirag.train_management_system.dto.PaymentResponseDto;
import com.chirag.train_management_system.dto.RefundResponseDto;
import com.chirag.train_management_system.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PaymentResponseDto> makePayment(
            @Valid @RequestBody PaymentRequestDto dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.makePayment(dto, authentication));
    }

    @PostMapping("/refund/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<RefundResponseDto> processRefund(
            @PathVariable Long bookingId,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.processRefund(bookingId, authentication));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PaymentResponseDto> getPaymentByBookingId(
            @PathVariable Long bookingId,
            Authentication authentication) {
        return ResponseEntity.ok(
                paymentService.getPaymentByBookingId(bookingId, authentication));
    }

    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> getPaymentByTransactionId(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(
                paymentService.getPaymentByTransactionId(transactionId));
    }
}