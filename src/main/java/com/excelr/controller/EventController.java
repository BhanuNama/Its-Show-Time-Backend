package com.excelr.controller;

import com.excelr.entity.BookingEntity;
import com.excelr.entity.EventEntity;
import com.excelr.entity.Status;
import com.excelr.entity.UserEntity;
import com.excelr.entity.VenueEntity;
import com.excelr.repository.BookingRepository;
import com.excelr.repository.EventRepository;
import com.excelr.repository.UserRepository;
import com.excelr.repository.VenueRepository;
import com.excelr.service.impl.EventServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Event controller for creating and listing events (concerts, shows, etc.)
 *
 * The detailed event structure (dates, zones, pricing) is stored in a single
 * JSON string field `eventConfig` directly on the entity to keep the model small.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EventController {

    private final EventServiceImpl eventService;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;

    @PostMapping
    public ResponseEntity<EventEntity> createEvent(@RequestBody CreateEventRequest request) {
        UserEntity owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + request.ownerId()));
        VenueEntity venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found with id: " + request.venueId()));

        EventEntity event = EventEntity.builder()
                .owner(owner)
                .venue(venue)
                .title(request.title())
                .description(request.description())
                .posterUrl(request.posterUrl())
                .address(request.address())
                .eventConfig(request.eventConfig())
                .status(Status.ACTIVE)
                .build();

        EventEntity saved = eventService.createEvent(event, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<EventEntity>> getAllEvents() {
        // Return all events (any status) for listing on user side
        // Repository has @EntityGraph on findAll() to eagerly fetch venue
        List<EventEntity> events = eventRepository.findAll();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventEntity> getEvent(@PathVariable Long id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<EventWithRevenue>> getEventsForOwner(@PathVariable Long ownerId) {
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + ownerId));
        List<EventEntity> events = eventService.getEventsByOwner(owner);
        
        // Calculate revenue for each event
        List<EventWithRevenue> eventsWithRevenue = events.stream()
                .map(event -> {
                    // Manually initialize venue to avoid lazy loading issues
                    if (event.getVenue() != null) {
                        event.getVenue().getName(); // Touch venue to initialize it
                    }
                    
                    // Calculate total revenue from confirmed bookings for this event
                    List<BookingEntity> confirmedBookings = bookingRepository.findByEventAndStatus(event, Status.CONFIRMED);
                    double totalRevenue = confirmedBookings.stream()
                            .mapToDouble(b -> b.getTotalAmount() != null ? b.getTotalAmount().doubleValue() : 0.0)
                            .sum();
                    
                    // Count total bookings
                    long totalBookings = confirmedBookings.size();
                    
                    return new EventWithRevenue(event, totalRevenue, totalBookings);
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(eventsWithRevenue);
    }

    public record CreateEventRequest(
            Long ownerId,
            Long venueId,
            String title,
            String description,
            String posterUrl,
            String address,
            String eventConfig   // JSON string from frontend describing dates/zones/categories
    ) {}
    
    public record EventWithRevenue(
            EventEntity event,
            double totalRevenue,
            long totalBookings
    ) {}
}

