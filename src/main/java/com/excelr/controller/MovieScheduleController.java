package com.excelr.controller;

import com.excelr.entity.MovieScheduleEntity;
import com.excelr.entity.ShowEntity;
import com.excelr.entity.Status;
import com.excelr.entity.VenueEntity;
import com.excelr.repository.MovieScheduleRepository;
import com.excelr.repository.VenueRepository;
import com.excelr.service.impl.MovieScheduleServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Movie scheduling controller used by the Owner Smart Scheduler.
 *
 * Main responsibilities:
 * - Create a schedule for a movie in a venue (startDate/endDate/showtimes/prices)
 * - Generate individual show instances for that schedule
 */
@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MovieScheduleController {

    private final MovieScheduleServiceImpl movieScheduleService;
    private final MovieScheduleRepository movieScheduleRepository;
    private final VenueRepository venueRepository;

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(@RequestBody CreateScheduleRequest request) {
        VenueEntity venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found with id: " + request.venueId()));

        MovieScheduleEntity schedule = MovieScheduleEntity.builder()
                .venue(venue)
                .tmdbMovieId(request.tmdbMovieId())
                .startDate(LocalDate.parse(request.startDate()))
                .endDate(LocalDate.parse(request.endDate()))
                .showtimes(toJsonArray(request.showtimes()))
                .silverPrice(request.silverPrice() != null ? request.silverPrice() : BigDecimal.ZERO)
                .goldPrice(request.goldPrice() != null ? request.goldPrice() : BigDecimal.ZERO)
                .vipPrice(request.vipPrice() != null ? request.vipPrice() : BigDecimal.ZERO)
                .status(Status.ACTIVE)
                .build();

        MovieScheduleEntity savedSchedule = movieScheduleService.createSchedule(schedule);

        // Generate the individual show instances for the selected window
        List<ShowEntity> shows = movieScheduleService.generateShowsForSchedule(savedSchedule);

        ScheduleResponse response = new ScheduleResponse(savedSchedule.getId(), shows.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/venue/{venueId}")
    public ResponseEntity<List<MovieScheduleEntity>> getSchedulesForVenue(@PathVariable Long venueId,
                                                                          @RequestParam(required = false) String fromDate) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found with id: " + venueId));

        LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : LocalDate.now();
        return ResponseEntity.ok(movieScheduleService.getActiveSchedulesForVenueFromDate(venue, from));
    }

    // Convert list of times to a simple JSON array string: ["09:00 AM","12:00 PM"]
    private String toJsonArray(List<String> times) {
        if (times == null || times.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < times.size(); i++) {
            sb.append("\"").append(times.get(i)).append("\"");
            if (i < times.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    // ===== Request / Response DTOs =====

    public record CreateScheduleRequest(
            Long venueId,
            Long tmdbMovieId,
            String startDate,        // ISO yyyy-MM-dd
            String endDate,          // ISO yyyy-MM-dd
            List<String> showtimes,  // ["09:00 AM", "12:00 PM", ...]
            BigDecimal silverPrice,
            BigDecimal goldPrice,
            BigDecimal vipPrice
    ) {}

    public record ScheduleResponse(
            Long scheduleId,
            int generatedShowCount
    ) {}
}

