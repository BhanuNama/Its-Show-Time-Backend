package com.excelr.repository;

import com.excelr.entity.WishlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistEntity, Long> {
    List<WishlistEntity> findByUserId(Long userId);
    Optional<WishlistEntity> findByUserIdAndItemIdAndType(Long userId, String itemId, String type);
    void deleteByUserIdAndItemIdAndType(Long userId, String itemId, String type);
}
