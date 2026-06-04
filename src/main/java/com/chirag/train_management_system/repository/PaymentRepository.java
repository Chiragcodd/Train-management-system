package com.chirag.train_management_system.repository;

import com.chirag.train_management_system.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByBookingBookingId(Long bookingId);

    boolean existsByBookingBookingId(Long bookingId);
}