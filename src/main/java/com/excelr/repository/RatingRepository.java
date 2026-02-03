package com.excelr.repository;

import com.excelr.entity.RatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<RatingEntity, Long> {

    /**
     * Find user's rating for a specific item
     */
    Optional<RatingEntity> findByUserIdAndItemIdAndItemType(Long userId, String itemId, String itemType);

    /**
     * Find all ratings for an item
     */
    List<RatingEntity> findByItemIdAndItemType(String itemId, String itemType);

    /**
     * Calculate average rating for an item
     */
    @Query("SELECT AVG(r.rating) FROM RatingEntity r WHERE r.itemId = :itemId AND r.itemType = :itemType")
    Double findAverageRatingByItemIdAndItemType(@Param("itemId") String itemId, @Param("itemType") String itemType);

    /**
     * Count total ratings for an item
     */
    Long countByItemIdAndItemType(String itemId, String itemType);

    /**
     * Check if user has already rated an item
     */
    boolean existsByUserIdAndItemIdAndItemType(Long userId, String itemId, String itemType);

    /**
     * Find all ratings by a user
     */
    List<RatingEntity> findByUserId(Long userId);
}
