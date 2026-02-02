package com.excelr.service;

import com.excelr.entity.UserEntity;
import com.excelr.entity.WishlistEntity;
import com.excelr.repository.UserRepository;
import com.excelr.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private UserRepository userRepository;

    public List<WishlistEntity> getWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId);
    }

    @Transactional
    public WishlistEntity toggleWishlist(Long userId, WishlistEntity item) {
        Optional<WishlistEntity> existing = wishlistRepository.findByUserIdAndItemIdAndType(userId, item.getItemId(), item.getType());
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return null; // Removed
        } else {
            UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            item.setUser(user);
            return wishlistRepository.save(item);
        }
    }

    @Transactional
    public void removeFromWishlist(Long userId, String itemId, String type) {
         wishlistRepository.deleteByUserIdAndItemIdAndType(userId, itemId, type);
    }
}
