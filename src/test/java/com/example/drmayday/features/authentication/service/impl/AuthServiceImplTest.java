package com.example.drmayday.features.authentication.service.impl;

import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.auth.config.AuthenticationProperties;
import com.example.drmayday.features.auth.dto.ForgotPasswordRequest;
import com.example.drmayday.features.auth.dto.ResetPasswordRequest;
import com.example.drmayday.features.auth.service.PasswordResetSmsSender;
import com.example.drmayday.features.auth.service.impl.AuthServiceImpl;
import com.example.drmayday.features.security.JwtTokenProvider;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordResetSmsSender passwordResetSmsSender;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authService = new AuthServiceImpl(
                userAccountRepository,
                redisTemplate,
                passwordEncoder,
                jwtTokenProvider,
                passwordResetSmsSender,
                new AuthenticationProperties(5)
        );
    }

    @Test
    void storesHashedResetCodeInRedisWithConfiguredExpiration() {
        UserAccount account = UserAccount.builder().accountId(42L).build();
        when(userAccountRepository.findByPerson_PhoneNumber("0912345678"))
                .thenReturn(Optional.of(account));

        authService.forgotPassword(new ForgotPasswordRequest("0912345678"));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> expirationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), expirationCaptor.capture());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetSmsSender).send(
                eq("0912345678"),
                codeCaptor.capture(),
                eq(5L)
        );

        assertEquals("password-reset:token:42", keyCaptor.getValue());
        assertTrue(passwordEncoder.matches(codeCaptor.getValue(), valueCaptor.getValue()));
        assertEquals(Duration.ofMinutes(5), expirationCaptor.getValue());
    }

    @Test
    void resetsPasswordAndConsumesToken() {
        UserAccount account = UserAccount.builder()
                .accountId(42L)
                .passwordHash(passwordEncoder.encode("old-password"))
                .build();
        when(userAccountRepository.findByPerson_PhoneNumber("0912345678"))
                .thenReturn(Optional.of(account));
        when(valueOperations.get("password-reset:token:42"))
                .thenReturn(passwordEncoder.encode("123456"));
        when(redisTemplate.delete("password-reset:token:42")).thenReturn(true);

        authService.resetPassword(new ResetPasswordRequest(
                "0912345678", "123456", "new-password", "new-password"));

        assertTrue(passwordEncoder.matches("new-password", account.getPasswordHash()));
        verify(userAccountRepository).save(account);
        verify(redisTemplate).delete("password-reset:token:42");
        verify(jwtTokenProvider).invalidateUserTokens(42L);
    }

    @Test
    void rejectsInvalidResetTokenWithoutChangingPassword() {
        UserAccount account = UserAccount.builder()
                .accountId(42L)
                .passwordHash(passwordEncoder.encode("old-password"))
                .build();
        String originalPasswordHash = account.getPasswordHash();
        when(userAccountRepository.findByPerson_PhoneNumber("0912345678"))
                .thenReturn(Optional.of(account));
        when(valueOperations.get("password-reset:token:42"))
                .thenReturn(passwordEncoder.encode("123456"));

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(
                new ResetPasswordRequest("0912345678", "654321", "new-password", "new-password")));

        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exception.getErrorCode());
        assertEquals(originalPasswordHash, account.getPasswordHash());
        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verify(redisTemplate, never()).delete("password-reset:token:42");
    }

    @Test
    void rejectsMismatchedPasswordConfirmationWithoutConsumingToken() {
        UserAccount account = UserAccount.builder()
                .accountId(42L)
                .passwordHash(passwordEncoder.encode("old-password"))
                .build();
        String originalPasswordHash = account.getPasswordHash();
        when(userAccountRepository.findByPerson_PhoneNumber("0912345678"))
                .thenReturn(Optional.of(account));
        when(valueOperations.get("password-reset:token:42"))
                .thenReturn(passwordEncoder.encode("123456"));

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(
                new ResetPasswordRequest("0912345678", "123456", "new-password", "different-password")));

        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exception.getErrorCode());
        assertEquals(originalPasswordHash, account.getPasswordHash());
        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verify(redisTemplate, never()).delete("password-reset:token:42");
    }
}
