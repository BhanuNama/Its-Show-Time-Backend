package com.excelr.service.impl;

import com.excelr.entity.BookingEntity;
import com.excelr.entity.UserEntity;
import com.excelr.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl {

    private final BookingRepository bookingRepository;

    public BookingEntity createBooking(BookingEntity booking, UserEntity user) {
        booking.setUser(user);
        if (booking.getBookedAt() == null) {
            booking.setBookedAt(LocalDateTime.now());
        }
        return bookingRepository.save(booking);
    }

    public List<BookingEntity> getBookingsForUser(UserEntity user) {
        return bookingRepository.findByUser(user);
    }
}

