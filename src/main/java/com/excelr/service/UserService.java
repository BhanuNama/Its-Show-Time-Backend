package com.excelr.service;

import com.excelr.entity.UserEntity;

public interface UserService {

    UserEntity registerUser(UserEntity user);

    UserEntity loginUser(String email, String password);

    boolean isEmailExists(String email);
}
