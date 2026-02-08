package com.excelr.controller;

import com.excelr.entity.BookingEntity;
import com.excelr.entity.ShowEntity;
import com.excelr.entity.VenueEntity;
import com.excelr.repository.BookingRepository;
import com.excelr.repository.ShowRepository;
import com.excelr.repository.VenueRepository;
import com.excelr.repository.MovieScheduleRepository;
import com.excelr.entity.MovieScheduleEntity;
import com.excelr.service.impl.ShowServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for reading show information used in user booking flows.
 *
 * Supports:
 * - Listing shows for a venue and date (for TheatreSelection step).
 */
@RestController
@RequestMapping("/api/shows")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ShowController {

        private final ShowServiceImpl showService;
        private final ShowRepository showRepository;
        private final VenueRepository venueRepository;
        private final BookingRepository bookingRepository;
        private final MovieScheduleRepository movieScheduleRepository;

        @GetMapping("/venue/{venueId}")
        public ResponseEntity<List<ShowSummary>> getShowsForVenueAndDate(@PathVariable Long venueId,
                        @RequestParam String date,
                        @RequestParam(required = false) Long movieId) {
                VenueEntity venue = venueRepository.findById(venueId)
                                .orElseThrow(() -> new IllegalArgumentException("Venue not found with id: " + venueId));

                LocalDate showDate = LocalDate.parse(date);
                List<ShowEntity> shows;

                // If movieId is provided, filter by movie; otherwise get all shows for
                // venue/date
                if (movieId != null) {
                        shows = showService.getShowsForVenueDateAndMovie(venue, showDate, movieId);
                } else {
                        shows = showService.getShowsForVenueAndDate(venue, showDate);
                }

                // Return a light-weight summary that lines up with TheatreSelection
                // expectations.
                List<ShowSummary> result = shows.stream()
                                .map(s -> new ShowSummary(
                                                s.getId(),
                                                s.getVenue().getName(),
                                                s.getShowTime(),
                                                s.getSilverPrice(),
                                                s.getGoldPrice(),
                                                s.getVipPrice()))
                                .collect(Collectors.toList());

                return ResponseEntity.ok(result);
        }

        /**
         * Summary of all movies that currently have shows, used by the owner dashboard.
         *
         * Aggregates ShowEntity rows by tmdbMovieId and returns:
         * - tmdbMovieId
         * - total number of shows
         * - first and last show dates
         * - total bookings across all those shows
         */
        @GetMapping("/summary")
        public ResponseEntity<List<MovieShowSummary>> getMovieShowSummary(
                        @RequestParam(required = false) Long ownerId) {
                List<ShowEntity> shows;
                if (ownerId != null) {
                        shows = showRepository.findByVenue_OwnerId(ownerId);
                } else {
                        // Return all shows for public view
                        shows = showRepository.findAll();
                }

                Map<Long, List<ShowEntity>> grouped = shows.stream()
                                .collect(Collectors.groupingBy(ShowEntity::getTmdbMovieId));

                List<MovieShowSummary> result = grouped.entrySet().stream()
                                .map(entry -> {
                                        Long tmdbMovieId = entry.getKey();
                                        List<ShowEntity> group = entry.getValue();

                                        LocalDate firstDate = group.stream()
                                                        .map(ShowEntity::getShowDate)
                                                        .min(Comparator.naturalOrder())
                                                        .orElse(null);

                                        LocalDate lastDate = group.stream()
                                                        .map(ShowEntity::getShowDate)
                                                        .max(Comparator.naturalOrder())
                                                        .orElse(null);

                                        long totalBookings = bookingRepository.countByShowIn(group);

                                        // Calculate total revenue from confirmed bookings for these shows
                                        List<BookingEntity> confirmedBookings = bookingRepository.findByShowInAndStatus(
                                                        group, com.excelr.entity.Status.CONFIRMED);
                                        double totalRevenue = confirmedBookings.stream()
                                                        .mapToDouble(b -> b.getTotalAmount() != null
                                                                        ? b.getTotalAmount().doubleValue()
                                                                        : 0.0)
                                                        .sum();

                                        // Get unique theatre names for this movie
                                        String theatres = group.stream()
                                                        .map(show -> show.getVenue().getName())
                                                        .distinct()
                                                        .collect(Collectors.joining(", "));

                                        // Only expose metrics if a specific owner is requesting their dashboard
                                        // If this is public view (ownerId == null), mask these values
                                        long safeBookings = (ownerId != null) ? totalBookings : 0;
                                        double safeRevenue = (ownerId != null) ? totalRevenue : 0.0;

                                        return new MovieShowSummary(
                                                        tmdbMovieId,
                                                        group.size(),
                                                        firstDate,
                                                        lastDate,
                                                        safeBookings,
                                                        safeRevenue,
                                                        theatres);
                                })
                                .toList();

                return ResponseEntity.ok(result);
        }

        @DeleteMapping("/movie/{tmdbMovieId}")
        public ResponseEntity<?> deleteMovieForOwner(
                        @PathVariable Long tmdbMovieId,
                        @RequestParam Long ownerId) {

                // 1. Find all shows for this movie and owner
                List<ShowEntity> shows = showRepository.findByTmdbMovieIdAndVenue_OwnerId(tmdbMovieId, ownerId);
                if (shows.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                // 2. Check for confirmed bookings (revenue > 0)
                List<BookingEntity> confirmedBookings = bookingRepository.findByShowInAndStatus(
                                shows, com.excelr.entity.Status.CONFIRMED);

                if (!confirmedBookings.isEmpty()) {
                        // If even one confirmed booking exists, we cannot delete (revenue generated)
                        return ResponseEntity.badRequest()
                                        .body("Cannot delete movie. There are active bookings generating revenue.");
                }

                // 3. Delete any cancelled/failed bookings to clear FK constraints
                List<BookingEntity> allBookings = bookingRepository.findByShowIn(shows);
                if (!allBookings.isEmpty()) {
                        bookingRepository.deleteAll(allBookings);
                }

                // 4. Identify schedules to delete (orphan cleanup)
                List<MovieScheduleEntity> schedules = shows.stream()
                                .map(ShowEntity::getSchedule)
                                .distinct()
                                .toList();

                // 5. Delete shows
                showRepository.deleteAll(shows);

                // 6. Delete schedules
                try {
                        movieScheduleRepository.deleteAll(schedules);
                } catch (Exception e) {
                        // ignore
                }

                return ResponseEntity.ok().build();
        }

        public record ShowSummary(
                        Long id,
                        String theatreName,
                        String time,
                        Object silverPrice,
                        Object goldPrice,
                        Object vipPrice) {
        }

        public record MovieShowSummary(
                        Long tmdbMovieId,
                        int showCount,
                        LocalDate firstShowDate,
                        LocalDate lastShowDate,
                        long totalBookings,
                        double totalRevenue,
                        String theatres) {
        }
}
