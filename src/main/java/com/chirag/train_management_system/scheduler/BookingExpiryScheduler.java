package com.chirag.train_management_system.scheduler;

import com.chirag.train_management_system.entity.Booking;
import com.chirag.train_management_system.enums.BookingStatus;
import com.chirag.train_management_system.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireUnpaidBookings() {
        List<Booking> expired =
                bookingRepository.findExpiredPendingBookings(LocalDateTime.now());
        if (expired.isEmpty()) return;

        expired.forEach(b -> {
            b.setStatus(BookingStatus.EXPIRED);
            log.info("Auto-expired | BookingId={} | ExpiresAt={}",
                    b.getBookingId(), b.getPaymentExpiresAt());
        });

        bookingRepository.saveAll(expired);
        log.info("Expiry scheduler | {} booking(s) expired.", expired.size());
    }
}