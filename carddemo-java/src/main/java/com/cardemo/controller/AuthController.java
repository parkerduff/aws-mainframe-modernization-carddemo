package com.cardemo.controller;

import com.cardemo.dto.request.LoginRequest;
import com.cardemo.dto.response.LoginResponse;
import com.cardemo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller - migrated from COBOL COSGN00C.cbl (CC00 signon transaction).
 * Replaces the CICS signon screen (COSGN00 BMS map).
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication (migrated from COSGN00C signon)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in", description = "Authenticate user and return JWT token (replaces CICS CC00 signon)")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
