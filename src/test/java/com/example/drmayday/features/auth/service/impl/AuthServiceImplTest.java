package com.example.drmayday.features.auth.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.Role;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.auth.config.AuthenticationProperties;
import com.example.drmayday.features.auth.dto.ChangePasswordRequest;
import com.example.drmayday.features.auth.dto.ForgotPasswordRequest;
import com.example.drmayday.features.auth.dto.LoginRequest;
import com.example.drmayday.features.auth.dto.LoginResponse;
import com.example.drmayday.features.auth.dto.ResetPasswordRequest;
import com.example.drmayday.features.auth.service.PasswordResetSmsSender;
import com.example.drmayday.features.security.JwtTokenProvider;
import com.example.drmayday.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String PHONE = "0912345678";
    private static final String RESET_TOKEN_KEY = "password-reset:token:42";
    private static final String AUTHORIZATION_HEADER = "Bearer access-token";
    private static final String OLD_PASSWORD_HASH = "encoded:old-password";
    private static final String NEW_PASSWORD_HASH = "encoded:new-password";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordResetSmsSender passwordResetSmsSender;

    private final long resetTokenExpirationMinutes = 5L;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userAccountRepository,
                redisTemplate,
                passwordEncoder,
                jwtTokenProvider,
                passwordResetSmsSender,
                new AuthenticationProperties(resetTokenExpirationMinutes)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("[Happy Path] Valid active account — returns token and user info")
        void login_validActiveAccount_returnsTokenAndUserInfo() {
            UserAccount account = account(true);
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches("correct-password", OLD_PASSWORD_HASH)).thenReturn(true);
            when(jwtTokenProvider.generateToken(42L, PHONE, "admin")).thenReturn("signed-token");

            LoginResponse response = authService.login(new LoginRequest(
                    " " + PHONE + " ", "correct-password"));

            assertEquals("signed-token", response.token());
            assertEquals(42L, response.userInfo().userId());
            assertEquals("Test User", response.userInfo().fullName());
            assertEquals(PHONE, response.userInfo().phone());
            assertEquals(7L, response.userInfo().roleId());
            verify(userAccountRepository).findByPerson_PhoneNumber(PHONE);
            verify(jwtTokenProvider).generateToken(42L, PHONE, "admin");
        }

        @Test
        @DisplayName("[Unhappy Path] Account not found — throws user account not found")
        void login_accountNotFound_throwsUserAccountNotFound() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.empty());

            assertAppException(
                    ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> authService.login(new LoginRequest(PHONE, "correct-password"))
            );

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).generateToken(any(Long.class), anyString(), anyString());
        }

        @Test
        @DisplayName("[Unhappy Path] Inactive account — throws password incorrect")
        void login_inactiveAccount_throwsPasswordIncorrect() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account(false)));

            assertAppException(
                    ErrorCode.PASSWORD_INCORRECT,
                    () -> authService.login(new LoginRequest(PHONE, "correct-password"))
            );

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).generateToken(any(Long.class), anyString(), anyString());
        }

        @Test
        @DisplayName("[Unhappy Path] Incorrect password — throws password incorrect")
        void login_incorrectPassword_throwsPasswordIncorrect() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.matches("wrong-password", OLD_PASSWORD_HASH)).thenReturn(false);

            assertAppException(
                    ErrorCode.PASSWORD_INCORRECT,
                    () -> authService.login(new LoginRequest(PHONE, "wrong-password"))
            );

            verify(jwtTokenProvider, never()).generateToken(any(Long.class), anyString(), anyString());
        }

        @Test
        @DisplayName("[Alternate Path] Null active account — throws password incorrect")
        void login_nullActiveAccount_throwsPasswordIncorrect() {
            UserAccount account = account(null);
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account));

            assertAppException(
                    ErrorCode.PASSWORD_INCORRECT,
                    () -> authService.login(new LoginRequest(PHONE, "correct-password"))
            );

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).generateToken(any(Long.class), anyString(), anyString());
        }

        @Test
        @DisplayName("[Exception Path] Repository throws — propagates exception")
        void login_repositoryThrows_propagatesException() {
            IllegalStateException failure = new IllegalStateException("repository unavailable");
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenThrow(failure);

            IllegalStateException thrown = assertThrows(
                    IllegalStateException.class,
                    () -> authService.login(new LoginRequest(PHONE, "correct-password"))
            );

            assertEquals(failure, thrown);
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).generateToken(any(Long.class), anyString(), anyString());
        }

        @Test
        @DisplayName("[Exception Path] Password encoder throws — propagates exception")
        void login_passwordEncoderThrows_propagatesException() {
            IllegalStateException failure = new IllegalStateException("encoder unavailable");
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.matches("password", OLD_PASSWORD_HASH)).thenThrow(failure);

            IllegalStateException thrown = assertThrows(
                    IllegalStateException.class,
                    () -> authService.login(new LoginRequest(PHONE, "password"))
            );

            assertEquals(failure, thrown);
            verify(jwtTokenProvider, never()).generateToken(any(Long.class), anyString(), anyString());
        }

        @Test
        @DisplayName("[Exception Path] Token provider throws — propagates exception")
        void login_tokenProviderThrows_propagatesException() {
            IllegalStateException failure = new IllegalStateException("token provider unavailable");
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.matches("password", OLD_PASSWORD_HASH)).thenReturn(true);
            when(jwtTokenProvider.generateToken(42L, PHONE, "admin")).thenThrow(failure);

            IllegalStateException thrown = assertThrows(
                    IllegalStateException.class,
                    () -> authService.login(new LoginRequest(PHONE, "password"))
            );

            assertEquals(failure, thrown);
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("[Happy Path] Valid authorization header — invalidates token")
        void logout_validAuthorizationHeader_invalidatesToken() {
            authService.logout(AUTHORIZATION_HEADER);

            verify(jwtTokenProvider).invalidateToken(AUTHORIZATION_HEADER);
        }

        @Test
        @DisplayName("[Alternate Path] Null authorization header — forwards header to provider")
        void logout_nullAuthorizationHeader_forwardsHeaderToProvider() {
            authService.logout(null);

            verify(jwtTokenProvider).invalidateToken(null);
        }

        @Test
        @DisplayName("[Exception Path] Token provider throws — propagates exception")
        void logout_tokenProviderThrows_propagatesException() {
            IllegalArgumentException failure = new IllegalArgumentException("invalid token");
            doThrow(failure).when(jwtTokenProvider).invalidateToken(AUTHORIZATION_HEADER);

            IllegalArgumentException thrown = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.logout(AUTHORIZATION_HEADER)
            );

            assertEquals(failure, thrown);
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("[Happy Path] Existing account — stores encoded code and sends sms")
        void forgotPassword_existingAccount_storesEncodedCodeAndSendsSms() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account(true)));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(passwordEncoder.encode(anyString())).thenAnswer(invocation ->
                    "encoded:" + invocation.<String>getArgument(0));

            authService.forgotPassword(new ForgotPasswordRequest(" " + PHONE + " "));

            ArgumentCaptor<String> storedKey = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> storedValue = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> expiration = ArgumentCaptor.forClass(Duration.class);
            verify(valueOperations).set(storedKey.capture(), storedValue.capture(), expiration.capture());

            ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
            verify(passwordResetSmsSender).send(eq(PHONE), sentCode.capture(), eq(resetTokenExpirationMinutes));
            assertEquals("password-reset:token:42", storedKey.getValue());
            assertTrue(sentCode.getValue().matches("[0-9]{6}"));
            assertEquals("encoded:" + sentCode.getValue(), storedValue.getValue());
            assertEquals(Duration.ofMinutes(resetTokenExpirationMinutes), expiration.getValue());
            verify(passwordEncoder).encode(sentCode.getValue());
        }

        @Test
        @DisplayName("[Unhappy Path] Account not found — throws user account not found")
        void forgotPassword_accountNotFound_throwsUserAccountNotFound() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.empty());

            assertAppException(
                    ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> authService.forgotPassword(new ForgotPasswordRequest(PHONE))
            );

            verify(redisTemplate, never()).opsForValue();
            verify(passwordResetSmsSender, never()).send(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("[Exception Path] Repository throws — propagates exception without side effects")
        void forgotPassword_repositoryThrows_propagatesExceptionWithoutSideEffects() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.forgotPassword(new ForgotPasswordRequest(PHONE))
            );

            verify(redisTemplate, never()).opsForValue();
            verify(passwordResetSmsSender, never()).send(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("[Exception Path] Password encoder throws — does not write or send")
        void forgotPassword_passwordEncoderThrows_doesNotWriteOrSend() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.encode(anyString())).thenThrow(new IllegalStateException("encoder unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.forgotPassword(new ForgotPasswordRequest(PHONE))
            );

            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
            verify(passwordResetSmsSender, never()).send(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("[Exception Path] Redis write throws — does not send sms")
        void forgotPassword_redisWriteThrows_doesNotSendSms() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account(true)));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-code");
            doThrow(new IllegalStateException("redis unavailable"))
                    .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.forgotPassword(new ForgotPasswordRequest(PHONE))
            );

            verify(passwordResetSmsSender, never()).send(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("[Exception Path] Sms sender throws — propagates exception after redis write")
        void forgotPassword_smsSenderThrows_propagatesExceptionAfterRedisWrite() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account(true)));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-code");
            IllegalStateException failure = new IllegalStateException("sms provider unavailable");
            doThrow(failure).when(passwordResetSmsSender)
                    .send(eq(PHONE), anyString(), eq(resetTokenExpirationMinutes));

            IllegalStateException thrown = assertThrows(
                    IllegalStateException.class,
                    () -> authService.forgotPassword(new ForgotPasswordRequest(PHONE))
            );

            assertEquals(failure, thrown);
            verify(valueOperations).set(
                    eq(RESET_TOKEN_KEY),
                    eq("encoded-code"),
                    eq(Duration.ofMinutes(resetTokenExpirationMinutes))
            );
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("[Happy Path] Valid code and matching passwords — updates password and consumes code")
        void resetPassword_validCodeAndMatchingPasswords_updatesPasswordAndConsumesCode() {
            UserAccount account = account(true);
            stubResetPasswordAccount(account);
            stubValidResetCode();
            when(passwordEncoder.encode("new-password")).thenReturn(NEW_PASSWORD_HASH);
            when(redisTemplate.delete(RESET_TOKEN_KEY)).thenReturn(true);

            authService.resetPassword(new ResetPasswordRequest(
                    PHONE, "123456", "new-password", "new-password"));

            assertEquals(NEW_PASSWORD_HASH, account.getPasswordHash());
            InOrder order = inOrder(userAccountRepository, redisTemplate, jwtTokenProvider);
            order.verify(userAccountRepository).save(account);
            order.verify(redisTemplate).delete(RESET_TOKEN_KEY);
            order.verify(jwtTokenProvider).invalidateUserTokens(42L);
        }

        @Test
        @DisplayName("[Unhappy Path] Account not found — throws user account not found")
        void resetPassword_accountNotFound_throwsUserAccountNotFound() {
            when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.empty());

            assertAppException(
                    ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "new-password", "new-password"))
            );

            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("[Unhappy Path] Missing code — throws invalid parameter value")
        void resetPassword_missingCode_throwsInvalidParameterValue() {
            UserAccount account = account(true);
            stubResetPasswordAccount(account);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(RESET_TOKEN_KEY)).thenReturn(null);

            assertAppException(
                    ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "new-password", "new-password"))
            );

            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(redisTemplate, never()).delete(RESET_TOKEN_KEY);
            verify(jwtTokenProvider, never()).invalidateUserTokens(any(Long.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid code — throws invalid parameter value")
        void resetPassword_invalidCode_throwsInvalidParameterValue() {
            UserAccount account = account(true);
            stubResetPasswordAccount(account);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(RESET_TOKEN_KEY)).thenReturn("encoded:123456");
            when(passwordEncoder.matches("654321", "encoded:123456")).thenReturn(false);

            assertAppException(
                    ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "654321", "new-password", "new-password"))
            );

            verify(passwordEncoder, never()).encode(anyString());
            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(redisTemplate, never()).delete(RESET_TOKEN_KEY);
            verify(jwtTokenProvider, never()).invalidateUserTokens(any(Long.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Mismatched confirmation — throws invalid parameter value")
        void resetPassword_mismatchedConfirmation_throwsInvalidParameterValue() {
            UserAccount account = account(true);
            stubResetPasswordAccount(account);
            stubValidResetCode();

            assertAppException(
                    ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "new-password", "different-password"))
            );

            verify(passwordEncoder, never()).encode(anyString());
            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(redisTemplate, never()).delete(RESET_TOKEN_KEY);
            verify(jwtTokenProvider, never()).invalidateUserTokens(any(Long.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Delete returns false — throws invalid parameter value")
        void resetPassword_deleteReturnsFalse_throwsInvalidParameterValue() {
            resetPassword_deleteReturnsFalseOrNull_throwsInvalidParameterValue(false);
        }

        @Test
        @DisplayName("[Unhappy Path] Delete returns null — throws invalid parameter value")
        void resetPassword_deleteReturnsNull_throwsInvalidParameterValue() {
            resetPassword_deleteReturnsFalseOrNull_throwsInvalidParameterValue(null);
        }

        @Test
        @DisplayName("[Exception Path] Password encoder matches throws — propagates exception")
        void resetPassword_passwordEncoderMatchesThrows_propagatesException() {
            stubResetPasswordAccount(account(true));
            when(valueOperations.get(RESET_TOKEN_KEY)).thenReturn("encoded:123456");
            when(passwordEncoder.matches("123456", "encoded:123456"))
                    .thenThrow(new IllegalStateException("encoder unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "new-password", "new-password"))
            );

            verify(passwordEncoder, never()).encode(anyString());
            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(redisTemplate, never()).delete(RESET_TOKEN_KEY);
        }

        @Test
        @DisplayName("[Exception Path] Redis read throws — propagates exception")
        void resetPassword_redisReadThrows_propagatesException() {
            stubResetPasswordAccount(account(true));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(RESET_TOKEN_KEY)).thenThrow(new IllegalStateException("redis unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "new-password", "new-password"))
            );

            verify(userAccountRepository, never()).save(any(UserAccount.class));
        }

        @Test
        @DisplayName("[Exception Path] Password encoder throws — propagates exception")
        void resetPassword_passwordEncoderThrows_propagatesException() {
            stubResetPasswordAccount(account(true));
            stubValidResetCode();
            when(passwordEncoder.encode("new-password"))
                    .thenThrow(new IllegalStateException("encoder unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "new-password", "new-password"))
            );

            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(redisTemplate, never()).delete(RESET_TOKEN_KEY);
        }

        @Test
        @DisplayName("[Exception Path] Repository save throws — propagates exception")
        void resetPassword_repositorySaveThrows_propagatesException() {
            UserAccount account = account(true);
            stubResetPasswordAccount(account);
            stubValidResetCode();
            when(passwordEncoder.encode("new-password")).thenReturn(NEW_PASSWORD_HASH);
            when(userAccountRepository.save(account))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "new-password", "new-password"))
            );

            verify(redisTemplate, never()).delete(RESET_TOKEN_KEY);
            verify(jwtTokenProvider, never()).invalidateUserTokens(any(Long.class));
        }

        @Test
        @DisplayName("[Exception Path] Redis delete throws — propagates exception")
        void resetPassword_redisDeleteThrows_propagatesException() {
            stubResetPasswordAccount(account(true));
            stubValidResetCode();
            when(passwordEncoder.encode("new-password")).thenReturn(NEW_PASSWORD_HASH);
            when(redisTemplate.delete(RESET_TOKEN_KEY))
                    .thenThrow(new IllegalStateException("redis unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "new-password", "new-password"))
            );

            verify(userAccountRepository).save(any(UserAccount.class));
            verify(jwtTokenProvider, never()).invalidateUserTokens(any(Long.class));
        }

        @Test
        @DisplayName("[Exception Path] Token invalidation throws — propagates exception")
        void resetPassword_tokenInvalidationThrows_propagatesException() {
            stubResetPasswordAccount(account(true));
            stubValidResetCode();
            when(passwordEncoder.encode("new-password")).thenReturn(NEW_PASSWORD_HASH);
            when(redisTemplate.delete(RESET_TOKEN_KEY)).thenReturn(true);
            doThrow(new IllegalStateException("token invalidation unavailable"))
                    .when(jwtTokenProvider).invalidateUserTokens(42L);

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "new-password", "new-password"))
            );

            verify(userAccountRepository).save(any(UserAccount.class));
            verify(redisTemplate).delete(RESET_TOKEN_KEY);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("[Happy Path] Valid current password — updates password and invalidates tokens")
        void changePassword_validCurrentPassword_updatesPasswordAndInvalidatesTokens() {
            UserAccount account = account(true);
            authenticateAs(42L);
            when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches("old-password", OLD_PASSWORD_HASH)).thenReturn(true);
            when(passwordEncoder.encode("new-password")).thenReturn(NEW_PASSWORD_HASH);

            authService.changePassword(AUTHORIZATION_HEADER,
                    new ChangePasswordRequest("old-password", "new-password", "new-password"));

            assertEquals(NEW_PASSWORD_HASH, account.getPasswordHash());
            InOrder order = inOrder(userAccountRepository, jwtTokenProvider);
            order.verify(userAccountRepository).save(account);
            order.verify(jwtTokenProvider).invalidateToken(AUTHORIZATION_HEADER);
            order.verify(jwtTokenProvider).invalidateUserTokens(42L);
        }

        @Test
        @DisplayName("[Unhappy Path] Account not found — throws user account not found")
        void changePassword_accountNotFound_throwsUserAccountNotFound() {
            authenticateAs(42L);
            when(userAccountRepository.findById(42L)).thenReturn(Optional.empty());

            assertAppException(
                    ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("old-password", "new-password", "new-password"))
            );

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).invalidateToken(anyString());
        }

        @Test
        @DisplayName("[Unhappy Path] Incorrect current password — throws password incorrect")
        void changePassword_incorrectCurrentPassword_throwsPasswordIncorrect() {
            authenticateAs(42L);
            when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.matches("wrong-password", OLD_PASSWORD_HASH)).thenReturn(false);

            assertAppException(
                    ErrorCode.PASSWORD_INCORRECT,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("wrong-password", "new-password", "new-password"))
            );

            verify(passwordEncoder, never()).encode(anyString());
            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(jwtTokenProvider, never()).invalidateToken(anyString());
        }

        @Test
        @DisplayName("[Unhappy Path] Mismatched confirmation — throws invalid parameter value")
        void changePassword_mismatchedConfirmation_throwsInvalidParameterValue() {
            authenticateAs(42L);
            when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.matches("old-password", OLD_PASSWORD_HASH)).thenReturn(true);

            assertAppException(
                    ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("old-password", "new-password", "different-password"))
            );

            verify(passwordEncoder, never()).encode(anyString());
            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(jwtTokenProvider, never()).invalidateToken(anyString());
        }

        @Test
        @DisplayName("[Alternate Path] Null security context — throws user not authenticated")
        void changePassword_nullSecurityContext_throwsUserNotAuthenticated() {
            SecurityContextHolder.clearContext();

            assertAppException(
                    ErrorCode.USER_NOT_AUTHENTICATED,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("old-password", "new-password", "new-password"))
            );

            verify(userAccountRepository, never()).findById(any(Long.class));
        }

        @Test
        @DisplayName("[Exception Path] Repository lookup throws — propagates exception")
        void changePassword_repositoryLookupThrows_propagatesException() {
            authenticateAs(42L);
            when(userAccountRepository.findById(42L))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("old-password", "new-password", "new-password"))
            );

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).invalidateToken(anyString());
        }

        @Test
        @DisplayName("[Exception Path] Password encoder matches throws — propagates exception")
        void changePassword_passwordEncoderMatchesThrows_propagatesException() {
            authenticateAs(42L);
            when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.matches("old-password", OLD_PASSWORD_HASH))
                    .thenThrow(new IllegalStateException("encoder unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("old-password", "new-password", "new-password"))
            );

            verify(passwordEncoder, never()).encode(anyString());
            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(jwtTokenProvider, never()).invalidateToken(anyString());
        }

        @Test
        @DisplayName("[Exception Path] Password encoder throws — propagates exception")
        void changePassword_passwordEncoderThrows_propagatesException() {
            authenticateAs(42L);
            when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.matches("old-password", OLD_PASSWORD_HASH)).thenReturn(true);
            when(passwordEncoder.encode("new-password"))
                    .thenThrow(new IllegalStateException("encoder unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("old-password", "new-password", "new-password"))
            );

            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(jwtTokenProvider, never()).invalidateToken(anyString());
        }

        @Test
        @DisplayName("[Exception Path] Repository save throws — propagates exception")
        void changePassword_repositorySaveThrows_propagatesException() {
            UserAccount account = account(true);
            authenticateAs(42L);
            when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches("old-password", OLD_PASSWORD_HASH)).thenReturn(true);
            when(passwordEncoder.encode("new-password")).thenReturn(NEW_PASSWORD_HASH);
            when(userAccountRepository.save(account))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("old-password", "new-password", "new-password"))
            );

            verify(jwtTokenProvider, never()).invalidateToken(anyString());
        }

        @Test
        @DisplayName("[Exception Path] Token invalidation throws — skips user token invalidation")
        void changePassword_tokenInvalidationThrows_skipsUserTokenInvalidation() {
            authenticateAs(42L);
            when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.matches("old-password", OLD_PASSWORD_HASH)).thenReturn(true);
            when(passwordEncoder.encode("new-password")).thenReturn(NEW_PASSWORD_HASH);
            doThrow(new IllegalStateException("token invalidation unavailable"))
                    .when(jwtTokenProvider).invalidateToken(AUTHORIZATION_HEADER);

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("old-password", "new-password", "new-password"))
            );

            verify(userAccountRepository).save(any(UserAccount.class));
            verify(jwtTokenProvider, never()).invalidateUserTokens(any(Long.class));
        }

        @Test
        @DisplayName("[Exception Path] User token invalidation throws — propagates exception")
        void changePassword_userTokenInvalidationThrows_propagatesException() {
            authenticateAs(42L);
            when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account(true)));
            when(passwordEncoder.matches("old-password", OLD_PASSWORD_HASH)).thenReturn(true);
            when(passwordEncoder.encode("new-password")).thenReturn(NEW_PASSWORD_HASH);
            doThrow(new IllegalStateException("user token invalidation unavailable"))
                    .when(jwtTokenProvider).invalidateUserTokens(42L);

            assertThrows(
                    IllegalStateException.class,
                    () -> authService.changePassword(AUTHORIZATION_HEADER,
                            new ChangePasswordRequest("old-password", "new-password", "new-password"))
            );

            verify(jwtTokenProvider).invalidateToken(AUTHORIZATION_HEADER);
        }
    }

    private void resetPassword_deleteReturnsFalseOrNull_throwsInvalidParameterValue(Boolean deleteResult) {
        UserAccount account = account(true);
        stubResetPasswordAccount(account);
        stubValidResetCode();
        when(passwordEncoder.encode("new-password")).thenReturn(NEW_PASSWORD_HASH);
        when(redisTemplate.delete(RESET_TOKEN_KEY)).thenReturn(deleteResult);

        assertAppException(
                ErrorCode.INVALID_PARAMETER_VALUE,
                () -> authService.resetPassword(new ResetPasswordRequest(
                        PHONE, "123456", "new-password", "new-password"))
        );

        verify(userAccountRepository).save(account);
        verify(jwtTokenProvider, never()).invalidateUserTokens(any(Long.class));
    }

    private void stubResetPasswordAccount(UserAccount account) {
        when(userAccountRepository.findByPerson_PhoneNumber(PHONE)).thenReturn(Optional.of(account));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private void stubValidResetCode() {
        when(valueOperations.get(RESET_TOKEN_KEY)).thenReturn("encoded:123456");
        when(passwordEncoder.matches("123456", "encoded:123456")).thenReturn(true);
    }

    private void authenticateAs(Long userId) {
        UserPrincipal principal = UserPrincipal.create(userId, PHONE, "admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private UserAccount account(Boolean active) {
        Person person = Person.builder()
                .personId(9L)
                .fullName("Test User")
                .phoneNumber(PHONE)
                .build();
        Role role = Role.builder()
                .roleId(7L)
                .name("admin")
                .build();
        return UserAccount.builder()
                .accountId(42L)
                .person(person)
                .role(role)
                .active(active)
                .passwordHash(OLD_PASSWORD_HASH)
                .build();
    }

    private AppException assertAppException(ErrorCode expectedCode, Executable action) {
        AppException exception = assertThrows(AppException.class, action);
        assertEquals(expectedCode, exception.getErrorCode());
        assertNotNull(exception.getMessage());
        return exception;
    }
}
