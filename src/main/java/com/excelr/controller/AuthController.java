package com.excelr.controller;

import com.excelr.entity.UserEntity;
import com.excelr.repository.UserRepository;
import com.excelr.security.JwtUtils;
import com.excelr.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller with JWT support.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final UserServiceImpl userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<UserEntity> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        UserEntity user = UserEntity.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password()) // Service handles encoding
                .role(request.role())
                .theatreName(request.theatreName())
                .phone(request.phone())
                .location(request.location())
                .build();

        UserEntity saved = userService.registerUser(user);
        saved.setPassword(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        final UserEntity user = userRepository.findByEmail(request.email()).orElseThrow();

        // Add extra claims if needed
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("role", user.getRole());

        String token = jwtUtils.generateToken(extraClaims, userDetails);

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()));
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<UserEntity> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.name() != null)
            user.setName(request.name());
        if (request.phone() != null)
            user.setPhone(request.phone());
        if (request.location() != null)
            user.setLocation(request.location());
        if (request.profileImageUrl() != null)
            user.setProfileImageUrl(request.profileImageUrl());

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

    // ===== DTOs =====

    public record RegisterRequest(
            String name,
            String email,
            String password,
            String role,
            String theatreName,
            String phone,
            String location) {
    }

    public record LoginRequest(
            String email,
            String password) {
    }

    public record AuthResponse(
            String token,
            Long id,
            String name,
            String email,
            String role) {
    }

    public record UpdateUserRequest(
            String name,
            String phone,
            String location,
            String profileImageUrl) {
    }
}
