package com.example.societyhub.service;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final DBHandler dbHandler;
    private final PasswordService passwordService;

    public AuthService(DBHandler dbHandler, PasswordService passwordService) {
        this.dbHandler = dbHandler;
        this.passwordService = passwordService;
    }

    public boolean validateAdminLogin(String email, String rawPassword) throws Exception {
        String storedHash = dbHandler.getPasswordByEmail(email);

        if (storedHash == null) return false;

        return passwordService.matches(rawPassword, storedHash);
    }
}

