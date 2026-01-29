package com.excelr.controller;

import com.excelr.entity.EventEntity;
import com.excelr.entity.UserEntity;
import com.excelr.repository.EventRepository;
import com.excelr.repository.UserRepository;
import com.excelr.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class ImageUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    private ResponseEntity<?> validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("Only image files are allowed");
        }
        
        return null;
    }

    private Map<String, String> createSuccessResponse(String message, String imageUrl) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put("imageUrl", imageUrl);
        return response;
    }

    @PostMapping("/profile/{userId}")
    public ResponseEntity<?> uploadProfileImage(
            @PathVariable Long userId,
            @RequestParam("image") MultipartFile file) {
        try {
            ResponseEntity<?> validationError = validateImageFile(file);
            if (validationError != null) return validationError;

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String fileUrl = fileStorageService.storeFile(file, "profiles");
            user.setProfileImageUrl(fileUrl);
            userRepository.save(user);

            return ResponseEntity.ok(createSuccessResponse("Profile image uploaded successfully", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    @PostMapping("/event/{eventId}")
    public ResponseEntity<?> uploadEventImage(
            @PathVariable Long eventId,
            @RequestParam("image") MultipartFile file) {
        try {
            ResponseEntity<?> validationError = validateImageFile(file);
            if (validationError != null) return validationError;

            EventEntity event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            String fileUrl = fileStorageService.storeFile(file, "events");
            event.setPosterUrl(fileUrl);
            eventRepository.save(event);

            return ResponseEntity.ok(createSuccessResponse("Event image uploaded successfully", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    @PostMapping("/event")
    public ResponseEntity<?> uploadEventImageOnly(@RequestParam("image") MultipartFile file) {
        try {
            ResponseEntity<?> validationError = validateImageFile(file);
            if (validationError != null) return validationError;

            String fileUrl = fileStorageService.storeFile(file, "events");
            return ResponseEntity.ok(createSuccessResponse("Image uploaded successfully", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to upload image: " + e.getMessage());
        }
    }
}
