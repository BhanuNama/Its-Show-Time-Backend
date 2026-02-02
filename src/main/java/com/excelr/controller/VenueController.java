package com.excelr.controller;

import com.excelr.entity.UserEntity;
import com.excelr.entity.VenueEntity;
import com.excelr.entity.VenueType;
import com.excelr.repository.UserRepository;
import com.excelr.repository.VenueRepository;
import com.excelr.service.impl.VenueServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for theatre / event-ground venues used in Owner dashboard.
 *
 * Covers:
 * - Creating a venue for an owner
 * - Listing venues for an owner
 * - Fetching a single venue
 */
@RestController
@RequestMapping("/api/venues")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VenueController {

    private final VenueServiceImpl venueService;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<VenueEntity> createVenue(@RequestBody CreateVenueRequest request) {
        UserEntity owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + request.ownerId()));

        // Pincode: strictly 6-digit numeric (India)
        String pincode = request.pincode() != null ? request.pincode().trim() : "";
        if (pincode.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("X-Error", "Pincode is required").body(null);
        }
        if (!pincode.matches("^\\d{6}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("X-Error", "Pincode must be exactly 6 digits").body(null);
        }

        VenueEntity venue = VenueEntity.builder()
                .owner(owner)
                .name(request.name())
                .type(VenueType.valueOf(request.type()))
                .location(request.location())
                .address(request.address())
                .pincode(pincode)
                .country(request.country())
                .capacity(request.capacity())
                .amenities(request.amenities())
                .build();

        VenueEntity saved = venueService.createVenue(venue, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVenue(
            @PathVariable Long id,
            @RequestBody CreateVenueRequest request
    ) {
        Optional<VenueEntity> venueOpt = venueRepository.findById(id);
        if (venueOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        VenueEntity venue = venueOpt.get();

        // Ownership guard (required for edit)
        if (request.ownerId() == null || venue.getOwner() == null || venue.getOwner().getId() == null
                || !venue.getOwner().getId().equals(request.ownerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not allowed to edit this venue");
        }

        // Pincode: strictly 6-digit numeric (India)
        String pincode = request.pincode() != null ? request.pincode().trim() : "";
        if (pincode.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("X-Error", "Pincode is required").body(null);
        }
        if (!pincode.matches("^\\d{6}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("X-Error", "Pincode must be exactly 6 digits").body(null);
        }

        venue.setName(request.name());
        venue.setType(VenueType.valueOf(request.type()));
        venue.setLocation(request.location());
        venue.setAddress(request.address());
        venue.setPincode(pincode);
        venue.setCountry(request.country());
        venue.setCapacity(request.capacity());
        venue.setAmenities(request.amenities());

        VenueEntity saved = venueRepository.save(venue);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<VenueEntity>> getVenuesForOwner(@PathVariable Long ownerId) {
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + ownerId));

        return ResponseEntity.ok(venueService.getVenuesByOwner(owner));
    }

    @GetMapping
    public ResponseEntity<List<VenueEntity>> getAllVenues() {
        return ResponseEntity.ok(venueRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueEntity> getVenue(@PathVariable Long id) {
        return venueRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVenue(
            @PathVariable Long id,
            @RequestParam(required = false) Long ownerId
    ) {
        Optional<VenueEntity> venueOpt = venueRepository.findById(id);
        if (venueOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        VenueEntity venue = venueOpt.get();

        // Basic ownership guard (prevents deleting other owner's venue from UI)
        if (ownerId != null && venue.getOwner() != null && venue.getOwner().getId() != null) {
            if (!venue.getOwner().getId().equals(ownerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not allowed to delete this venue");
            }
        }

        try {
            venueRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            // Likely FK constraint (shows/schedules/events reference this venue)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete venue because it is referenced by schedules/shows/events");
        }
    }

    public record CreateVenueRequest(
            Long ownerId,
            String name,
            String type,       // "THEATRE" | "EVENT_GROUND"
            String location,
            String address,
            String pincode,
            String country,
            Integer capacity,
            String amenities    // JSON string or comma separated, depending on use
    ) {}
}

