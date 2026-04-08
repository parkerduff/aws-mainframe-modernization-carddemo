package com.cardemo.service;

import com.cardemo.dto.request.LoginRequest;
import com.cardemo.dto.response.LoginResponse;
import com.cardemo.entity.User;
import com.cardemo.repository.UserRepository;
import com.cardemo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId("USER0001");
        testUser.setFirstName("JOHN");
        testUser.setLastName("DOE");
        testUser.setPassword("encoded_password");
        testUser.setUserType("A");
    }

    @Test
    void login_withValidCredentials_returnsLoginResponse() {
        when(userRepository.findByUserId("USER0001")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded_password")).thenReturn(true);
        when(tokenProvider.generateToken("USER0001", "A")).thenReturn("jwt_token");

        LoginResponse response = authService.login(new LoginRequest("USER0001", "password"));

        assertEquals("USER0001", response.userId());
        assertEquals("A", response.userType());
        assertEquals("JOHN", response.firstName());
        assertEquals("jwt_token", response.token());
    }

    @Test
    void login_withInvalidUser_throwsBadCredentials() {
        when(userRepository.findByUserId("INVALID")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("INVALID", "password")));
    }

    @Test
    void login_withInvalidPassword_throwsBadCredentials() {
        when(userRepository.findByUserId("USER0001")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("USER0001", "wrong")));
    }
}
