package com.cardemo.service;

import com.cardemo.dto.request.LoginRequest;
import com.cardemo.dto.response.LoginResponse;
import com.cardemo.entity.User;
import com.cardemo.repository.UserRepository;
import com.cardemo.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication service - migrated from COBOL program COSGN00C.cbl (CC00 transaction).
 * Handles user login validation against the user security store (formerly USRSEC VSAM file).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUserId(request.userId().toUpperCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Failed login attempt for user: {}", request.userId());
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = tokenProvider.generateToken(user.getUserId(), user.getUserType());
        log.info("User logged in: {} (type: {})", user.getUserId(), user.getUserType());

        return new LoginResponse(
                user.getUserId(),
                user.getUserType(),
                user.getFirstName(),
                user.getLastName(),
                token
        );
    }
}
