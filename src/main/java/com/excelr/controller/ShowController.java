package com.excelr.controller;

import com.excelr.entity.BookingEntity;
import com.excelr.entity.ShowEntity;
import com.excelr.entity.VenueEntity;
import com.excelr.repository.BookingRepository;
import com.excelr.repository.ShowRepository;
import com.excelr.repository.VenueRepository;
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

    @GetMapping("/venue/{venueId}")
    public ResponseEntity<List<ShowSummary>> getShowsForVenueAndDate(@PathVariable Long venueId,
                                                                      @RequestParam String date,
                                                                      @RequestParam(required = false) Long movieId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found with id: " + venueId));

        LocalDate showDate = LocalDate.parse(date);
        List<ShowEntity> shows;
        
        // If movieId is provided, filter by movie; otherwise get all shows for venue/date
        if (movieId != null) {
            shows = showService.getShowsForVenueDateAndMovie(venue, showDate, movieId);
        } else {
            shows = showService.getShowsForVenueAndDate(venue, showDate);
        }

        // Return a light-weight summary that lines up with TheatreSelection expectations.
        List<ShowSummary> result = shows.stream()
                .map(s -> new ShowSummary(
                        s.getId(),
                        s.getVenue().getName(),
                        s.getShowTime(),
                        s.getSilverPrice(),
                        s.getGoldPrice(),
                        s.getVipPrice()
                ))
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
    public ResponseEntity<List<MovieShowSummary>> getMovieShowSummary() {
        List<ShowEntity> shows = showRepository.findAll();

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
                    List<BookingEntity> confirmedBookings = bookingRepository.findByShowInAndStatus(group, com.excelr.entity.Status.CONFIRMED);
                    double totalRevenue = confirmedBookings.stream()
                            .mapToDouble(b -> b.getTotalAmount() != null ? b.getTotalAmount().doubleValue() : 0.0)
                            .sum();
                    
                    // Get unique theatre names for this movie
                    String theatres = group.stream()
                            .map(show -> show.getVenue().getName())
                            .distinct()
                            .collect(Collectors.joining(", "));

                    return new MovieShowSummary(
                            tmdbMovieId,
                            group.size(),
                            firstDate,
                            lastDate,
                            totalBookings,
                            totalRevenue,
                            theatres
                    );
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    public record ShowSummary(
            Long id,
            String theatreName,
            String time,
            Object silverPrice,
            Object goldPrice,
            Object vipPrice
    ) {}

    public record MovieShowSummary(
            Long tmdbMovieId,
            int showCount,
            LocalDate firstShowDate,
            LocalDate lastShowDate,
            long totalBookings,
            double totalRevenue,
            String theatres
    ) {}
}

