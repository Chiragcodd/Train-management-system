// package com.chirag.train_management_system.controller;

// import com.chirag.train_management_system.dto.BookingRequestDto;
// import com.chirag.train_management_system.dto.BookingResponseDto;
// import com.chirag.train_management_system.service.BookingService;
// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import org.springframework.data.domain.Page;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.security.core.Authentication;
// import org.springframework.web.bind.annotation.*;

// @RestController
// @RequestMapping("/api/bookings")
// @RequiredArgsConstructor
// public class BookingController {

//     private final BookingService bookingService;

//     @PostMapping
//     @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
//     public ResponseEntity<BookingResponseDto> bookTicket(
//             @Valid @RequestBody BookingRequestDto dto,
//             Authentication authentication) {
//         return ResponseEntity.status(HttpStatus.CREATED)
//                 .body(bookingService.bookTicket(dto, authentication));
//     }

//     @PutMapping("/{id}/cancel")
//     @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
//     public ResponseEntity<BookingResponseDto> cancelBooking(
//             @PathVariable Long id,
//             Authentication authentication) {
//         return ResponseEntity.ok(bookingService.cancelBooking(id, authentication));
//     }

//     @GetMapping("/{id}")
//     @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
//     public ResponseEntity<BookingResponseDto> getBookingById(
//             @PathVariable Long id,
//             Authentication authentication) {
//         return ResponseEntity.ok(bookingService.getBookingById(id, authentication));
//     }

//     @GetMapping("/pnr/{pnr}")
//     @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
//     public ResponseEntity<BookingResponseDto> getBookingByPnr(
//             @PathVariable String pnr,
//             Authentication authentication) {
//         return ResponseEntity.ok(bookingService.getBookingByPnr(pnr, authentication));
//     }

//     @GetMapping("/user/{userId}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<Page<BookingResponseDto>> getUserBookingsForAdmin(
//             @PathVariable Long userId,
//             @RequestParam(defaultValue = "0") int page,
//             @RequestParam(defaultValue = "10") int size,
//             Authentication authentication) {
//         return ResponseEntity.ok(
//                 bookingService.getUserBookingsForAdmin(userId, page, size));
//     }

//         @GetMapping("/my-bookings")
//         @PreAuthorize("hasRole('USER')")
//         public ResponseEntity<Page<BookingResponseDto>> getMyBookings(
//                 @RequestParam(defaultValue = "0") int page,
//                 @RequestParam(defaultValue = "10") int size,
//                 Authentication authentication) {
//             return ResponseEntity.ok(
//                     bookingService.getMyBookings(page, size, authentication));
//         }




//     @GetMapping
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<Page<BookingResponseDto>> getAllBookings(
//             @RequestParam(defaultValue = "0") int page,
//             @RequestParam(defaultValue = "10") int size) {
//         return ResponseEntity.ok(bookingService.getAllBookings(page, size));
//     }
// }


package com.chirag.train_management_system.controller;

import com.chirag.train_management_system.dto.BookingRequestDto;
import com.chirag.train_management_system.dto.BookingResponseDto;
import com.chirag.train_management_system.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BookingResponseDto> bookTicket(
            @Valid @RequestBody BookingRequestDto dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.bookTicket(dto, authentication));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BookingResponseDto> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BookingResponseDto> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.getBookingById(id, authentication));
    }

    @GetMapping("/pnr/{pnr}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BookingResponseDto> getBookingByPnr(
            @PathVariable String pnr,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.getBookingByPnr(pnr, authentication));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BookingResponseDto>> getUserBookingsForAdmin(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return ResponseEntity.ok(
                bookingService.getUserBookingsForAdmin(userId, page, size));
    }

        @GetMapping("/my-bookings")
        @PreAuthorize("hasRole('USER')")
        public ResponseEntity<Page<BookingResponseDto>> getMyBookings(
                @RequestParam(defaultValue = "0")   int page,
                @RequestParam(defaultValue = "10")  int size,
                @RequestParam(defaultValue = "ALL") String status,
                Authentication authentication) {
            return ResponseEntity.ok(
                    bookingService.getMyBookings(page, size, status, authentication));
        }




    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BookingResponseDto>> getAllBookings(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "ALL") String status) {
        return ResponseEntity.ok(bookingService.getAllBookings(page, size, status));
    }
}