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

    public BookingEntity cancelBooking(Long bookingId, UserEntity user) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied: You can only cancel your own bookings");
        }

        if (booking.getStatus() == com.excelr.entity.Status.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        // Check 30-minute cancellation window
        long minutesSinceBooking = java.time.Duration.between(booking.getBookedAt(), LocalDateTime.now()).toMinutes();
        if (minutesSinceBooking > 30) {
            throw new IllegalArgumentException("The cancellation period of 30min is completed");
        }

        // Proceed with cancellation
        booking.setStatus(com.excelr.entity.Status.CANCELLED);
        booking.setPaymentStatus(com.excelr.entity.PaymentStatus.REFUNDED);

        return bookingRepository.save(booking);
    }

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public java.util.Map<String, Object> getMovieAnalytics(Long movieId, Long ownerId) {
        List<BookingEntity> bookings;
        if (ownerId != null) {
            bookings = bookingRepository.findByShowTmdbMovieIdAndShowVenueOwnerIdAndStatus(movieId, ownerId,
                    com.excelr.entity.Status.CONFIRMED);
        } else {
            bookings = bookingRepository.findByShowTmdbMovieIdAndStatus(movieId, com.excelr.entity.Status.CONFIRMED);
        }
        return calculateAnalytics(bookings, true);
    }

    public java.util.Map<String, Object> getEventAnalytics(Long eventId, Long ownerId) {
        List<BookingEntity> bookings;
        if (ownerId != null) {
            bookings = bookingRepository.findByEventIdAndEventOwnerIdAndStatus(eventId, ownerId,
                    com.excelr.entity.Status.CONFIRMED);
        } else {
            bookings = bookingRepository.findByEventIdAndStatus(eventId, com.excelr.entity.Status.CONFIRMED);
        }
        return calculateAnalytics(bookings, false);
    }

    private java.util.Map<String, Object> calculateAnalytics(List<BookingEntity> bookings, boolean isMovie) {
        java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;
        int totalBookings = bookings.size();
        int totalSeats = 0;
        java.util.Map<String, java.util.Map<String, Object>> venueStats = new java.util.HashMap<>();

        for (BookingEntity b : bookings) {
            // Sum Revenue
            if (b.getTotalAmount() != null) {
                totalRevenue = totalRevenue.add(b.getTotalAmount());
            }

            // Count Seats
            int seatsInBooking = 0;
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> details = objectMapper.readValue(b.getBookingDetails(),
                        java.util.Map.class);

                if (isMovie) {
                    if (details.containsKey("seats")) {
                        @SuppressWarnings("unchecked")
                        List<String> seats = (List<String>) details.get("seats");
                        seatsInBooking = seats != null ? seats.size() : 0;
                    }
                } else {
                    // Event: sum zone quantities
                    if (details.containsKey("selectedZones")) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> zones = (java.util.Map<String, Object>) details
                                .get("selectedZones");
                        for (Object zoneVal : zones.values()) {
                            if (zoneVal instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> cats = (java.util.Map<String, Object>) zoneVal;
                                for (Object qty : cats.values()) {
                                    if (qty instanceof Number) {
                                        seatsInBooking += ((Number) qty).intValue();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore parsing error, count as 0
            }
            totalSeats += seatsInBooking;

            // Group by Venue
            String venueName = "Unknown Venue";
            if (isMovie && b.getShow() != null && b.getShow().getVenue() != null) {
                venueName = b.getShow().getVenue().getName();
            } else if (!isMovie && b.getEvent() != null) {
                if (b.getEvent().getVenue() != null) {
                    venueName = b.getEvent().getVenue().getName();
                } else if (b.getEvent().getAddress() != null) {
                    venueName = b.getEvent().getAddress();
                }
            }

            venueStats.putIfAbsent(venueName, new java.util.HashMap<>(java.util.Map.of(
                    "revenue", java.math.BigDecimal.ZERO,
                    "bookings", 0,
                    "seats", 0)));

            java.util.Map<String, Object> stats = venueStats.get(venueName);
            stats.put("revenue", ((java.math.BigDecimal) stats.get("revenue"))
                    .add(b.getTotalAmount() != null ? b.getTotalAmount() : java.math.BigDecimal.ZERO));
            stats.put("bookings", (Integer) stats.get("bookings") + 1);
            stats.put("seats", (Integer) stats.get("seats") + seatsInBooking);
        }

        return java.util.Map.of(
                "totalRevenue", totalRevenue,
                "totalBookings", totalBookings,
                "totalSeats", totalSeats,
                "revenueByVenue", venueStats);
    }
}
