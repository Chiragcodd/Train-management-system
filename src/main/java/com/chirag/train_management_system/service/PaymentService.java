package com.chirag.train_management_system.service;

import com.chirag.train_management_system.dto.*;
import com.chirag.train_management_system.entity.*;
import com.chirag.train_management_system.enums.*;
import com.chirag.train_management_system.exception.*;
import com.chirag.train_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public PaymentResponseDto makePayment(
            PaymentRequestDto dto, Authentication authentication) {

        Booking booking = findBookingById(dto.getBookingId());
        checkOwnershipOrAdmin(booking.getUser().getUsername(), authentication,
                "You can only pay for your own bookings.");

        if (booking.getStatus() == BookingStatus.CANCELLED)
            throw new PaymentException("Cannot pay for a cancelled booking.");
        if (booking.getStatus() == BookingStatus.EXPIRED)
            throw new PaymentException("Booking expired. Please book again.");
        if (booking.getStatus() == BookingStatus.CONFIRMED)
            throw new PaymentException(
                    "Payment already done. PNR: " + booking.getPnrNumber());

        // Allowed: PENDING_PAYMENT (mixed/all-CNF) or WAITLISTED (all-WL)
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getStatus() != BookingStatus.WAITLISTED)
            throw new PaymentException(
                    "Invalid status for payment: " + booking.getStatus());

        // Already paid WL booking
        if (booking.getStatus() == BookingStatus.WAITLISTED
                && booking.getPnrNumber() != null)
            throw new PaymentException(
                    "Already paid. PNR: " + booking.getPnrNumber());

        // Payment window
        if (booking.getPaymentExpiresAt() != null
                && LocalDateTime.now().isAfter(booking.getPaymentExpiresAt())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new PaymentException("Payment window expired. Please book again.");
        }

        // Duplicate payment check
        paymentRepository.findByBookingBookingId(booking.getBookingId())
                .ifPresent(ex -> {
                    if (ex.getStatus() == PaymentStatus.SUCCESS
                            || ex.getStatus() == PaymentStatus.REFUNDED)
                        throw new PaymentException(
                                "Already paid. TxnId: " + ex.getTransactionId());
                });

        // Amount match
        if (dto.getAmount() == null
                || dto.getAmount().compareTo(booking.getTotalAmount()) != 0)
            throw new PaymentException(
                    "Amount ₹" + dto.getAmount()
                    + " does not match ₹" + booking.getTotalAmount());

        // Build payment
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTransactionId(generateTransactionId(dto.getPaymentMethod()));
        payment.setStatus(PaymentStatus.SUCCESS);

        String pnr = generatePnr();
        booking.setPnrNumber(pnr);

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            // PENDING_PAYMENT → CONFIRMED
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentExpiresAt(null);
            log.info("Payment | PENDING_PAYMENT → CONFIRMED | PNR={}", pnr);
        } else {
            // WAITLISTED → WAITLISTED (status nahi badlega — seat baad mein milegi)
            booking.setPaymentExpiresAt(null);
            log.info("Payment | WAITLISTED paid | PNR={} | WL#={}",
                    pnr, booking.getWaitlistNumber());
        }

        bookingRepository.save(booking);
        Payment saved = paymentRepository.save(payment);

        log.info("Payment saved | TxnId={} | PNR={} | Amount=₹{}",
                saved.getTransactionId(), pnr, saved.getAmount());

        return toDto(saved);
    }

    @Transactional
    public RefundResponseDto processRefund(Long bookingId, Authentication auth) {
        Booking booking = findBookingById(bookingId);
        checkOwnershipOrAdmin(booking.getUser().getUsername(), auth,
                "You can only request refund for your own bookings.");

        if (booking.getStatus() != BookingStatus.CANCELLED)
            throw new PaymentException(
                    "Refund only for cancelled bookings. Status: " + booking.getStatus());

        Payment payment = paymentRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new PaymentException(
                        "No payment found. Nothing to refund."));

        if (payment.getStatus() == PaymentStatus.FAILED)
            throw new PaymentException("Original payment was not successful.");
        if (payment.getRefundTransactionId() != null)
            throw new PaymentException(
                    "Refund already processed. Txn: " + payment.getRefundTransactionId());

        BigDecimal refundAmount = booking.getRefundAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return RefundResponseDto.builder()
                    .paymentId(payment.getId())
                    .bookingId(bookingId)
                    .pnrNumber(booking.getPnrNumber())
                    .originalAmount(payment.getAmount())
                    .refundAmount(BigDecimal.ZERO)
                    .status(PaymentStatus.SUCCESS)
                    .message("No refund applicable.")
                    .build();
        }

        payment.setRefundAmount(refundAmount);
        payment.setRefundTransactionId(generateTransactionId("REFUND"));
        payment.setRefundDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);

        log.info("Refund | RefundTxn={} | PNR={} | Amount=₹{}",
                saved.getRefundTransactionId(), booking.getPnrNumber(), refundAmount);

        return RefundResponseDto.builder()
                .paymentId(saved.getId())
                .bookingId(bookingId)
                .pnrNumber(booking.getPnrNumber())
                .originalAmount(saved.getAmount())
                .refundAmount(saved.getRefundAmount())
                .refundTransactionId(saved.getRefundTransactionId())
                .status(saved.getStatus())
                .refundDate(saved.getRefundDate())
                .message("Refund of ₹" + saved.getRefundAmount()
                        + " will be credited in 5–7 business days.")
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByBookingId(Long bookingId, Authentication auth) {
        Booking booking = findBookingById(bookingId);
        checkOwnershipOrAdmin(booking.getUser().getUsername(), auth,
                "You can only view your own payment.");
        return toDto(paymentRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "booking id", String.valueOf(bookingId))));
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByTransactionId(String txnId) {
        return toDto(paymentRepository.findByTransactionId(txnId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "transaction id", txnId)));
    }

    private Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private void checkOwnershipOrAdmin(String owner, Authentication auth, String msg) {
        boolean isOwner = owner.equals(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin)
            throw new CustomAccessDeniedException(msg);
    }

    private String generatePnr() {
        return "PNR" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 10).toUpperCase();
    }

    private String generateTransactionId(String prefix) {
        String p    = prefix.replaceAll("[^A-Za-z]", "");
        String safe = p.isEmpty() ? "TXN"
                : p.substring(0, Math.min(p.length(), 3)).toUpperCase();
        return safe + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12).toUpperCase();
    }

    private PaymentResponseDto toDto(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentId(payment.getId());
        dto.setBookingId(payment.getBooking().getBookingId());
        dto.setPnrNumber(payment.getBooking().getPnrNumber());
        dto.setUserName(payment.getBooking().getUser().getName());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setTransactionId(payment.getTransactionId());
        dto.setStatus(payment.getStatus());
        dto.setPaymentDate(payment.getPaymentDate());
        return dto;
    }
}