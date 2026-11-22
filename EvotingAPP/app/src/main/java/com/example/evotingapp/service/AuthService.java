package com.example.evotingapp.service;

import com.example.evotingapp.repository.IUserRepository;
import com.example.evotingapp.role.User;

public class AuthService {
    private final IUserRepository userRepo;

    public AuthService(IUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new ElectionException("Username dan password wajib diisi.");
        }
        User user = userRepo.authenticate(username.trim(), password);
        if (user == null) {
            throw new ElectionException("Username atau password salah.");
        }
        return user;
    }
}
