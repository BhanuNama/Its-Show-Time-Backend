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
 * Authentication controller with JWT support and Email OTP.
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
    private final com.excelr.service.SmsService smsService;

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> request) {
        if (userRepository.existsByEmail(request.get("email"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already registered");
        }
        return ResponseEntity.ok().build();
    }

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

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
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

    @PostMapping("/send-sms-otp")
    public ResponseEntity<?> sendSmsOtp(@RequestBody SmsRequest request) {
        try {
            // Verify service returns SID now
            String sid = smsService.sendOtp(request.phone());
            Map<String, String> response = new HashMap<>();
            response.put("message", "OTP sent via Twilio Verify to " + request.phone());
            response.put("sid", sid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send OTP: " + e.getMessage());
        }
    }

    @PostMapping("/verify-sms-otp")
    public ResponseEntity<?> verifySmsOtp(@RequestBody VerifySmsRequest request) {
        boolean isValid = smsService.verifyOtp(request.phone(), request.otp());
        if (isValid) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP verified successfully");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP");
        }
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

    public record GoogleLoginRequest(String email, String uid) {
    }

    public record SmsRequest(String phone) {
    }

    public record VerifySmsRequest(String phone, String otp) {
    }
}
