package com.excelr.service.impl;

import com.excelr.entity.BookingEntity;
import com.excelr.entity.UserEntity;
import com.excelr.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl {

    private final BookingRepository bookingRepository;
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int CODE_LEN = 12; // BK + 12 chars => 14 total

    private String generateBookingCode() {
        char[] out = new char[2 + CODE_LEN];
        out[0] = 'B';
        out[1] = 'K';
        for (int i = 0; i < CODE_LEN; i++) {
            out[2 + i] = CODE_ALPHABET[RNG.nextInt(CODE_ALPHABET.length)];
        }
        return new String(out);
    }

    public BookingEntity createBooking(BookingEntity booking, UserEntity user) {
        booking.setUser(user);
        if (booking.getBookedAt() == null) {
            booking.setBookedAt(LocalDateTime.now());
        }
        if (booking.getBookingCode() == null || booking.getBookingCode().isBlank()) {
            // Generate a unique, non-sequential public booking code
            for (int i = 0; i < 10; i++) {
                String code = generateBookingCode();
                if (!bookingRepository.existsByBookingCode(code)) {
                    booking.setBookingCode(code);
                    break;
                }
            }
            if (booking.getBookingCode() == null || booking.getBookingCode().isBlank()) {
                throw new IllegalStateException("Failed to generate booking code");
            }
        }
        return bookingRepository.save(booking);
    }

    public List<BookingEntity> getBookingsForUser(UserEntity user) {
        return bookingRepository.findByUser(user);
    }
}

