package com.excelr.controller;

import com.excelr.service.impl.BookingServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnalyticsController {

    private final BookingServiceImpl bookingService;

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<Map<String, Object>> getMovieAnalytics(@PathVariable Long movieId,
            @RequestParam(required = false) Long ownerId) {
        return ResponseEntity.ok(bookingService.getMovieAnalytics(movieId, ownerId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<Map<String, Object>> getEventAnalytics(@PathVariable Long eventId,
            @RequestParam(required = false) Long ownerId) {
        return ResponseEntity.ok(bookingService.getEventAnalytics(eventId, ownerId));
    }
}
