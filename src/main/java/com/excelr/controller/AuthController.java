package com.excelr.controller;

import com.excelr.entity.UserEntity;
import com.excelr.repository.UserRepository;
import com.excelr.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simple authentication controller for registering and logging in users.
 *
 * NOTE:
 * - This is intentionally minimal: no JWT, no roles/guards yet.
 * - Frontend can call these endpoints instead of using localStorage-only auth.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final UserServiceImpl userService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<UserEntity> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        UserEntity user = UserEntity.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password()) // In real apps, hash this!
                .role(request.role())
                .theatreName(request.theatreName())
                .phone(request.phone())
                .location(request.location())
                .build();

        UserEntity saved = userService.registerUser(user);
        // Do not expose password back to client
        saved.setPassword(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<UserEntity> login(@RequestBody LoginRequest request) {
        UserEntity user = userService.loginUser(request.email(), request.password());
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<UserEntity> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.name() != null) user.setName(request.name());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.location() != null) user.setLocation(request.location());
        if (request.profileImageUrl() != null) user.setProfileImageUrl(request.profileImageUrl());

        UserEntity updated = userRepository.save(user);
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserEntity> getUser(@PathVariable Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // ===== Request DTOs kept simple and local to controller =====

    public record RegisterRequest(
            String name,
            String email,
            String password,
            String role,          // "USER" or "OWNER"
            String theatreName,
            String phone,
            String location
    ) {}

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record UpdateUserRequest(
            String name,
            String phone,
            String location,
            String profileImageUrl
    ) {}
}

