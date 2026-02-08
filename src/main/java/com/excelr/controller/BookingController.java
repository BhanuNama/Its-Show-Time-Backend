package com.excelr.controller;

import com.excelr.entity.*;
import com.excelr.repository.BookingRepository;
import com.excelr.repository.EventRepository;
import com.excelr.repository.ShowRepository;
import com.excelr.repository.UserRepository;
import com.excelr.service.impl.BookingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Booking controller for both movie and event bookings.
 *
 * Responsibilities:
 * - Create movie booking (seats + show)
 * - Create event booking (selected zones + date)
 * - List bookings for a user (for MyBookings screen)
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BookingController {

        private final BookingServiceImpl bookingService;
        private final BookingRepository bookingRepository;
        private final UserRepository userRepository;
        private final ShowRepository showRepository;
        private final EventRepository eventRepository;
        private final ObjectMapper objectMapper;

        private UserEntity requireAuthenticatedUser() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || auth.getName() == null) {
                        throw new IllegalArgumentException("Unauthorized");
                }
                return userRepository.findByEmail(auth.getName())
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }

        @PostMapping("/movie")
        public ResponseEntity<BookingEntity> createMovieBooking(@RequestBody MovieBookingRequest request) {
                UserEntity user = userRepository.findById(request.userId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User not found with id: " + request.userId()));

                ShowEntity show = null;
                if (request.showId() != null) {
                        show = showRepository.findById(request.showId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Show not found with id: " + request.showId()));
                }

                String bookingDetailsJson;
                try {
                        bookingDetailsJson = objectMapper.writeValueAsString(Map.of(
                                        "seats", request.seats()));
                } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize movie booking seats", e);
                }

                BookingEntity booking = BookingEntity.builder()
                                .user(user)
                                .type(BookingType.MOVIE)
                                .show(show)
                                .event(null)
                                .eventDateId(null)
                                .bookingDetails(bookingDetailsJson)
                                .totalAmount(request.totalAmount() != null ? request.totalAmount() : BigDecimal.ZERO)
                                .paymentMethod(request.paymentMethod())
                                .paymentStatus(PaymentStatus.COMPLETED)
                                .status(Status.CONFIRMED)
                                .bookedAt(LocalDateTime.now())
                                .build();

                BookingEntity saved = bookingService.createBooking(booking, user);
                return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }

        @PostMapping("/event")
        public ResponseEntity<BookingEntity> createEventBooking(@RequestBody EventBookingRequest request) {
                UserEntity user = userRepository.findById(request.userId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User not found with id: " + request.userId()));
                EventEntity event = eventRepository.findById(request.eventId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Event not found with id: " + request.eventId()));

                String bookingDetailsJson;
                try {
                        bookingDetailsJson = objectMapper.writeValueAsString(Map.of(
                                        "selectedZones", request.selectedZones()));
                } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize event booking zones", e);
                }

                BookingEntity booking = BookingEntity.builder()
                                .user(user)
                                .type(BookingType.EVENT)
                                .show(null)
                                .event(event)
                                .eventDateId(request.eventDateId())
                                .bookingDetails(bookingDetailsJson)
                                .totalAmount(request.totalAmount() != null ? request.totalAmount() : BigDecimal.ZERO)
                                .paymentMethod(request.paymentMethod())
                                .paymentStatus(PaymentStatus.COMPLETED)
                                .status(Status.CONFIRMED)
                                .bookedAt(LocalDateTime.now())
                                .build();

                BookingEntity saved = bookingService.createBooking(booking, user);
                return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }

        @GetMapping("/{id}")
        public ResponseEntity<BookingEntity> getBookingById(@PathVariable Long id) {
                BookingEntity booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
                UserEntity authUser = requireAuthenticatedUser();
                if (booking.getUser() == null || authUser.getId() == null
                                || !authUser.getId().equals(booking.getUser().getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                return ResponseEntity.ok(booking);
        }

        @PostMapping("/{id}/cancel")
        public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
                try {
                        UserEntity authUser = requireAuthenticatedUser();
                        BookingEntity cancelled = bookingService.cancelBooking(id, authUser);
                        return ResponseEntity.ok(cancelled);
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Failed to cancel booking"));
                }
        }

        /**
         * Public booking lookup by bookingCode for QR scans.
         * Returns a limited payload (no user info).
         *
         * Example: GET /api/bookings/public/BK8F3KZ1P9Q2X
         */
        @GetMapping("/public/{bookingCode}")
        public ResponseEntity<PublicBookingResponse> getBookingByCodePublic(@PathVariable String bookingCode) {
                BookingEntity booking = bookingRepository.findByBookingCode(bookingCode)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Booking not found with code: " + bookingCode));

                // Do not leak user info in public ticket scans
                return ResponseEntity.ok(new PublicBookingResponse(
                                booking.getId(),
                                booking.getBookingCode(),
                                booking.getType(),
                                booking.getShow(),
                                booking.getEvent(),
                                booking.getEventDateId(),
                                booking.getBookingDetails(),
                                booking.getTotalAmount(),
                                booking.getPaymentMethod(),
                                booking.getStatus(),
                                booking.getBookedAt()));
        }

        @GetMapping("/user/{userId}")
        public ResponseEntity<List<BookingEntity>> getBookingsForUser(@PathVariable Long userId) {
                UserEntity authUser = requireAuthenticatedUser();
                if (authUser.getId() == null || !authUser.getId().equals(userId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                UserEntity user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
                return ResponseEntity.ok(bookingService.getBookingsForUser(user));
        }

        @GetMapping("/movie/{movieId}")
        public ResponseEntity<List<BookingEntity>> getBookingsForMovie(@PathVariable Long movieId,
                        @RequestParam(required = false) Long ownerId) {
                if (ownerId != null) {
                        return ResponseEntity.ok(bookingRepository.findByShowTmdbMovieIdAndShowVenueOwnerIdAndStatus(
                                        movieId, ownerId, Status.CONFIRMED));
                }
                return ResponseEntity.ok(bookingRepository.findByShowTmdbMovieId(movieId));
        }

        @GetMapping("/event/{eventId}")
        public ResponseEntity<List<BookingEntity>> getBookingsForEvent(@PathVariable Long eventId,
                        @RequestParam(required = false) Long ownerId) {
                if (ownerId != null) {
                        return ResponseEntity.ok(bookingRepository.findByEventIdAndEventOwnerIdAndStatus(eventId,
                                        ownerId, Status.CONFIRMED));
                }
                return ResponseEntity.ok(bookingRepository.findByEventId(eventId));
        }

        @GetMapping
        public ResponseEntity<List<BookingSummary>> getAllBookings() {
                List<BookingEntity> all = bookingRepository.findAll();

                List<BookingSummary> result = all.stream()
                                .map(b -> new BookingSummary(
                                                b.getId(),
                                                b.getBookingCode(),
                                                b.getUser() != null ? b.getUser().getName() : null,
                                                b.getType(),
                                                b.getShow() != null
                                                                ? new ShowInfo(
                                                                                b.getShow().getId(),
                                                                                b.getShow().getTmdbMovieId(),
                                                                                b.getShow().getShowDate(),
                                                                                b.getShow().getShowTime(),
                                                                                b.getShow().getVenue() != null
                                                                                                ? new VenueInfo(
                                                                                                                b.getShow().getVenue()
                                                                                                                                .getId(),
                                                                                                                b.getShow().getVenue()
                                                                                                                                .getName())
                                                                                                : null)
                                                                : null,
                                                b.getEvent() != null
                                                                ? new EventInfo(
                                                                                b.getEvent().getId(),
                                                                                b.getEvent().getTitle(),
                                                                                b.getEvent().getEventConfig(),
                                                                                b.getEvent().getVenue() != null
                                                                                                ? new VenueInfo(
                                                                                                                b.getEvent()
                                                                                                                                .getVenue()
                                                                                                                                .getId(),
                                                                                                                b.getEvent()
                                                                                                                                .getVenue()
                                                                                                                                .getName())
                                                                                                : null,
                                                                                b.getEvent().getAddress(),
                                                                                b.getEvent().getPosterUrl())
                                                                : null,
                                                b.getBookingDetails(),
                                                b.getTotalAmount(),
                                                b.getStatus(),
                                                b.getBookedAt()))
                                .toList();

                return ResponseEntity.ok(result);
        }

        /**
         * Get blocked/booked seats for a specific show
         */
        @GetMapping("/show/{showId}/blocked-seats")
        public ResponseEntity<List<String>> getBlockedSeatsForShow(@PathVariable Long showId) {
                List<BookingEntity> bookings = bookingRepository.findByShowIdAndStatus(showId, Status.CONFIRMED);

                List<String> blockedSeats = bookings.stream()
                                .flatMap(booking -> {
                                        try {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> details = objectMapper.readValue(
                                                                booking.getBookingDetails(),
                                                                Map.class);
                                                @SuppressWarnings("unchecked")
                                                List<String> seats = (List<String>) details.get("seats");
                                                return seats != null ? seats.stream() : java.util.stream.Stream.empty();
                                        } catch (Exception e) {
                                                return java.util.stream.Stream.empty();
                                        }
                                })
                                .toList();

                return ResponseEntity.ok(blockedSeats);
        }

        /**
         * Get zone availability for a specific event and date
         */
        @GetMapping("/event/{eventId}/zone-availability")
        public ResponseEntity<Map<String, ZoneAvailability>> getZoneAvailabilityForEvent(
                        @PathVariable Long eventId,
                        @RequestParam String eventDateId) {
                EventEntity event = eventRepository.findById(eventId)
                                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));

                // Get all confirmed bookings for this event and date
                List<BookingEntity> bookings = bookingRepository.findByEventIdAndEventDateIdAndStatus(
                                eventId, eventDateId, Status.CONFIRMED);

                // Parse event config to get zone capacities
                Map<String, ZoneAvailability> zoneAvailability = new java.util.HashMap<>();
                try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> eventConfigMap = objectMapper.readValue(event.getEventConfig(), Map.class);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> zones = (List<Map<String, Object>>) eventConfigMap.get("zones");

                        for (Map<String, Object> zoneData : zones) {
                                String zoneName = (String) zoneData.get("name");
                                Integer capacity = (Integer) zoneData.get("capacity");

                                // Count booked passes for this zone
                                int bookedPasses = bookings.stream()
                                                .mapToInt(booking -> {
                                                        try {
                                                                @SuppressWarnings("unchecked")
                                                                Map<String, Object> details = objectMapper.readValue(
                                                                                booking.getBookingDetails(),
                                                                                Map.class);
                                                                @SuppressWarnings("unchecked")
                                                                Map<String, Object> selectedZones = (Map<String, Object>) details
                                                                                .get("selectedZones");
                                                                if (selectedZones != null && selectedZones
                                                                                .containsKey(zoneName)) {
                                                                        @SuppressWarnings("unchecked")
                                                                        Map<String, Object> zoneInfo = (Map<String, Object>) selectedZones
                                                                                        .get(zoneName);
                                                                        Object adultObj = zoneInfo.get("adult");
                                                                        Object childObj = zoneInfo.get("child");
                                                                        int adult = adultObj instanceof Integer
                                                                                        ? (Integer) adultObj
                                                                                        : 0;
                                                                        int child = childObj instanceof Integer
                                                                                        ? (Integer) childObj
                                                                                        : 0;
                                                                        return adult + child;
                                                                }
                                                                return 0;
                                                        } catch (Exception e) {
                                                                return 0;
                                                        }
                                                })
                                                .sum();

                                int available = capacity - bookedPasses;
                                zoneAvailability.put(zoneName, new ZoneAvailability(
                                                capacity,
                                                bookedPasses,
                                                available,
                                                available > 0));
                        }
                } catch (Exception e) {
                        throw new RuntimeException("Failed to parse event config", e);
                }

                return ResponseEntity.ok(zoneAvailability);
        }

        // ===== Request DTOs =====

        public record MovieBookingRequest(
                        Long userId,
                        Long showId,
                        List<String> seats,
                        BigDecimal totalAmount,
                        String paymentMethod // "card", "upi", "netbanking", "wallet"
        ) {
        }

        public record EventBookingRequest(
                        Long userId,
                        Long eventId,
                        String eventDateId,
                        Map<String, Map<String, Integer>> selectedZones,
                        BigDecimal totalAmount,
                        String paymentMethod // "card", "upi", "netbanking", "wallet"
        ) {
        }

        // ===== Response DTOs =====

        public record ShowInfo(
                        Long id,
                        Long tmdbMovieId,
                        LocalDate showDate,
                        String showTime,
                        VenueInfo venue) {
        }

        public record VenueInfo(
                        Long id,
                        String name) {
        }

        public record EventInfo(
                        Long id,
                        String title,
                        String eventConfig,
                        VenueInfo venue,
                        String address,
                        String posterUrl) {
        }

        public record BookingSummary(
                        Long id,
                        String bookingCode,
                        String userName,
                        BookingType type,
                        ShowInfo show,
                        EventInfo event,
                        String bookingDetails,
                        BigDecimal totalAmount,
                        Status status,
                        LocalDateTime bookedAt) {
        }

        public record PublicBookingResponse(
                        Long id,
                        String bookingCode,
                        BookingType type,
                        ShowEntity show,
                        EventEntity event,
                        String eventDateId,
                        String bookingDetails,
                        BigDecimal totalAmount,
                        String paymentMethod,
                        Status status,
                        LocalDateTime bookedAt) {
        }

        public record ZoneAvailability(
                        int capacity,
                        int booked,
                        int available,
                        boolean isAvailable) {
        }
}
