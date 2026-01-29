package com.excelr.service.impl;

import org.springframework.stereotype.Service;

import com.excelr.entity.UserEntity;
import com.excelr.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;

    public UserEntity registerUser(UserEntity user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        return userRepository.save(user);
    }

    public UserEntity loginUser(String email, String password) {
        return userRepository
                .findByEmail(email)
                .filter(user -> user.getPassword().equals(password))
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
    }
}
