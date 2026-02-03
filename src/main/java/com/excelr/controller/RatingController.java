package com.excelr.controller;

import com.excelr.entity.RatingEntity;
import com.excelr.entity.UserEntity;
import com.excelr.repository.RatingRepository;
import com.excelr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Rating controller for movies and events.
 * Allows users to rate items with 1-5 stars.
 */
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    private UserEntity getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    /**
     * Create or update a rating
     */
    @PostMapping
    public ResponseEntity<?> createOrUpdateRating(@RequestBody RatingRequest request) {
        UserEntity user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login to rate");
        }

        // Validate rating value
        if (request.rating() < 1 || request.rating() > 5) {
            return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
        }

        // Check if user already rated this item
        Optional<RatingEntity> existingRating = ratingRepository.findByUserIdAndItemIdAndItemType(
                user.getId(), request.itemId(), request.itemType());

        RatingEntity rating;
        if (existingRating.isPresent()) {
            // Update existing rating
            rating = existingRating.get();
            rating.setRating(request.rating());
            rating.setReview(request.review());
        } else {
            // Create new rating
            rating = RatingEntity.builder()
                    .user(user)
                    .itemId(request.itemId())
                    .itemType(request.itemType())
                    .rating(request.rating())
                    .review(request.review())
                    .build();
        }

        RatingEntity saved = ratingRepository.save(rating);
        return ResponseEntity.ok(new RatingResponse(
                saved.getId(),
                saved.getRating(),
                saved.getReview(),
                true));
    }

    /**
     * Get average rating and count for an item (public endpoint)
     */
    @GetMapping("/{itemType}/{itemId}")
    public ResponseEntity<RatingSummary> getRatingSummary(
            @PathVariable String itemType,
            @PathVariable String itemId) {

        Double avgRating = ratingRepository.findAverageRatingByItemIdAndItemType(itemId, itemType);
        Long count = ratingRepository.countByItemIdAndItemType(itemId, itemType);

        return ResponseEntity.ok(new RatingSummary(
                avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0,
                count != null ? count : 0L));
    }

    /**
     * Get current user's rating for an item
     */
    @GetMapping("/{itemType}/{itemId}/user")
    public ResponseEntity<RatingResponse> getUserRating(
            @PathVariable String itemType,
            @PathVariable String itemId) {

        UserEntity user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.ok(new RatingResponse(null, null, null, false));
        }

        Optional<RatingEntity> rating = ratingRepository.findByUserIdAndItemIdAndItemType(
                user.getId(), itemId, itemType);

        if (rating.isPresent()) {
            RatingEntity r = rating.get();
            return ResponseEntity.ok(new RatingResponse(r.getId(), r.getRating(), r.getReview(), true));
        }

        return ResponseEntity.ok(new RatingResponse(null, null, null, false));
    }

    /**
     * Delete user's rating
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRating(@PathVariable Long id) {
        UserEntity user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<RatingEntity> rating = ratingRepository.findById(id);
        if (rating.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check ownership
        if (!rating.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot delete another user's rating");
        }

        ratingRepository.delete(rating.get());
        return ResponseEntity.ok().build();
    }

    /**
     * Get all reviews for an item
     */
    @GetMapping("/{itemType}/{itemId}/reviews")
    public ResponseEntity<List<ReviewDto>> getReviews(
            @PathVariable String itemType,
            @PathVariable String itemId) {

        List<RatingEntity> ratings = ratingRepository.findByItemIdAndItemType(itemId, itemType);
        
        List<ReviewDto> reviews = ratings.stream()
                .filter(r -> r.getReview() != null && !r.getReview().trim().isEmpty())
                .map(r -> new ReviewDto(
                        r.getId(),
                        r.getUser().getName(),
                        r.getRating(),
                        r.getReview(),
                        r.getCreatedAt()
                ))
                .sorted((r1, r2) -> r2.createdAt().compareTo(r1.createdAt())) // Newest first
                .collect(Collectors.toList());

        return ResponseEntity.ok(reviews);
    }

    // ===== DTOs =====

    public record RatingRequest(
            String itemId,
            String itemType, // "movie" or "event"
            Integer rating,  // 1-5
            String review    // optional
    ) {}

    public record RatingResponse(
            Long id,
            Integer rating,
            String review,
            boolean hasRated
    ) {}

    public record RatingSummary(
            Double averageRating,
            Long totalRatings
    ) {}

    public record ReviewDto(
            Long id,
            String userName,
            Integer rating,
            String review,
            java.time.LocalDateTime createdAt
    ) {}
}
