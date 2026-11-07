package com.example.drmayday.features.authentication.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.PasswordResetToken;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.authentication.config.AuthenticationProperties;
import com.example.drmayday.features.authentication.dto.ChangePasswordRequest;
import com.example.drmayday.features.authentication.dto.ForgotPasswordRequest;
import com.example.drmayday.features.authentication.dto.LoginRequest;
import com.example.drmayday.features.authentication.dto.LoginResponse;
import com.example.drmayday.features.authentication.service.AuthService;
import com.example.drmayday.features.authentication.service.PasswordResetSmsSender;
import com.example.drmayday.features.security.JwtTokenProvider;
import com.example.drmayday.repository.PasswordResetTokenRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetSmsSender passwordResetSmsSender;
    private final long passwordResetTokenExpirationMinutes;

    public AuthServiceImpl(UserAccountRepository userAccountRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordResetSmsSender passwordResetSmsSender,
                           AuthenticationProperties authenticationProperties) {
        this.userAccountRepository = userAccountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordResetSmsSender = passwordResetSmsSender;
        this.passwordResetTokenExpirationMinutes = authenticationProperties.passwordResetTokenExpirationMinutes();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String phone = request.phone().trim();
        UserAccount account = userAccountRepository.findByPerson_PhoneNumber(phone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        if (!Boolean.TRUE.equals(account.getActive())
                || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }

        String token = jwtTokenProvider.generateToken(
                account.getAccountId(),
                phone,
                account.getRole().getName()
        );
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                account.getAccountId(),
                account.getPerson().getFullName(),
                phone,
                account.getRole().getRoleId()
        );
        return new LoginResponse(token, userInfo);
    }

    @Override
    @Transactional
    public void logout(String authorizationHeader) {
        jwtTokenProvider.invalidateToken(authorizationHeader);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String phone = request.phone().trim();
        UserAccount account = userAccountRepository.findByPerson_PhoneNumber(phone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(passwordResetTokenExpirationMinutes);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByUserId(account.getAccountId())
                .orElseGet(() -> new PasswordResetToken(account.getAccountId(), "", expiresAt));
        resetToken.setTokenHash(passwordEncoder.encode(code));
        resetToken.setExpiresAt(expiresAt);
        passwordResetTokenRepository.save(resetToken);

        passwordResetSmsSender.send(phone, code, passwordResetTokenExpirationMinutes);
    }

    @Override
    @Transactional
    public void changePassword(String authorizationHeader, ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(account);
        jwtTokenProvider.invalidateToken(authorizationHeader);
        jwtTokenProvider.invalidateUserTokens(userId);
    }
}
