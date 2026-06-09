package com.aws.carddemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aws.carddemo.dto.LoginRequest;
import com.aws.carddemo.dto.LoginResponse;
import com.aws.carddemo.entity.SecUserEntity;
import com.aws.carddemo.exception.AuthenticationFailedException;
import com.aws.carddemo.repository.SecUserRepository;
import com.aws.carddemo.security.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests for {@link AuthService}, covering the COSGN00C sign-on logic: success, user not
 * found, wrong password, and admin vs user routing.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SecUserRepository secUserRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(
            "dGVzdC1zZWNyZXQta2V5LWZvci1jYXJkZGVtby11bml0LXRlc3RzLTI1Ni1iaXQ=", 3600000L);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(secUserRepository, passwordEncoder, tokenProvider);
    }

    private SecUserEntity user(String id, String type, String rawPwd) {
        SecUserEntity u = new SecUserEntity();
        u.setUsrId(id);
        u.setUsrType(type);
        u.setUsrPwd(passwordEncoder.encode(rawPwd));
        return u;
    }

    @Test
    void loginSucceedsForAdminAndRoutesToAdminMenu() {
        when(secUserRepository.findById("ADMIN001")).thenReturn(Optional.of(user("ADMIN001", "A", "PASSWORD")));

        LoginResponse response = authService.login(new LoginRequest("admin001", "PASSWORD"));

        assertThat(response.userId()).isEqualTo("ADMIN001");
        assertThat(response.userType()).isEqualTo("A");
        assertThat(response.nextProgram()).isEqualTo("COADM01C");
        assertThat(response.token()).isNotBlank();
        assertThat(tokenProvider.getUserType(response.token())).isEqualTo("A");
    }

    @Test
    void loginSucceedsForRegularUserAndRoutesToUserMenu() {
        when(secUserRepository.findById("USER0001")).thenReturn(Optional.of(user("USER0001", "U", "PASSWORD")));

        LoginResponse response = authService.login(new LoginRequest("USER0001", "PASSWORD"));

        assertThat(response.userType()).isEqualTo("U");
        assertThat(response.nextProgram()).isEqualTo("COMEN01C");
    }

    @Test
    void loginFailsWhenUserNotFound() {
        when(secUserRepository.findById("NOBODY01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("NOBODY01", "PASSWORD")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void loginFailsWhenPasswordWrong() {
        when(secUserRepository.findById("USER0001")).thenReturn(Optional.of(user("USER0001", "U", "PASSWORD")));

        assertThatThrownBy(() -> authService.login(new LoginRequest("USER0001", "WRONGPWD")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Wrong Password");
    }
}
