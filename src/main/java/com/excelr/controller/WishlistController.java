package com.excelr.controller;

import com.excelr.entity.WishlistEntity;
import com.excelr.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "http://localhost:5173")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<WishlistEntity>> getWishlist(@PathVariable Long userId) {
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleWishlist(@RequestBody Map<String, Object> payload) {
        if (payload.get("userId") == null || payload.get("id") == null || payload.get("type") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields: userId, id, or type"));
        }

        Long userId = Long.valueOf(payload.get("userId").toString());
        String itemId = payload.get("id").toString();
        String type = payload.get("type").toString();
        
        String genre = "";
        if (payload.get("genre") instanceof List) {
           List<?> list = (List<?>) payload.get("genre");
           if (!list.isEmpty()) {
               genre = list.stream().map(Object::toString).reduce((a, b) -> a + ", " + b).orElse("");
           }
        } else if (payload.get("genre") != null) {
            genre = payload.get("genre").toString();
        }

        WishlistEntity item = WishlistEntity.builder()
                .itemId(itemId)
                .type(type)
                .title(payload.get("title") != null ? payload.get("title").toString() : "Unknown Title")
                .poster(payload.get("poster") != null ? payload.get("poster").toString() : null)
                .rating(payload.get("rating") != null ? Double.valueOf(payload.get("rating").toString()) : null)
                .duration((String) payload.get("duration"))
                .genre(genre)
                .venue((String) payload.get("venue"))
                .date((String) payload.get("date"))
                .build();

        WishlistEntity result = wishlistService.toggleWishlist(userId, item);
        if (result == null) {
            return ResponseEntity.ok(Map.of("status", "removed"));
        } else {
            return ResponseEntity.ok(Map.of("status", "added", "item", result));
        }
    }
    
    @DeleteMapping("/{userId}/{type}/{itemId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long userId, @PathVariable String type, @PathVariable String itemId) {
        wishlistService.removeFromWishlist(userId, itemId, type);
        return ResponseEntity.ok(Map.of("status", "removed"));
    }
}
