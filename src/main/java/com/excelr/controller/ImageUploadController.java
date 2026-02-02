package com.excelr.controller;

import com.excelr.entity.EventEntity;
import com.excelr.entity.UserEntity;
import com.excelr.repository.EventRepository;
import com.excelr.repository.UserRepository;
import com.excelr.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    /**
     * Proxy TMDB image downloads through backend to avoid browser CORS issues
     * when exporting ticket UI to an image.
     *
     * Allowed host: image.tmdb.org
     * Example: /api/upload/tmdb-proxy?url=https://image.tmdb.org/t/p/w500/xyz.jpg
     */
    @GetMapping("/tmdb-proxy")
    public ResponseEntity<byte[]> proxyTmdbImage(@RequestParam("url") String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new byte[0]);
            }
            if (uri.getHost() == null || !"image.tmdb.org".equalsIgnoreCase(uri.getHost())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new byte[0]);
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Its-Show-Time")
                    .GET()
                    .build();

            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]);
            }

            String contentType = resp.headers().firstValue("content-type").orElse("image/jpeg");
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
            headers.setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());

            return new ResponseEntity<>(resp.body(), headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new byte[0]);
        }
    }
}
