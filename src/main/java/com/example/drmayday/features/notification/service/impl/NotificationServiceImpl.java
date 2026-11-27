package com.example.drmayday.features.notification.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.Notification;
import com.example.drmayday.entity.UserFcmToken;
import com.example.drmayday.features.notification.dto.FcmTokenResponse;
import com.example.drmayday.features.notification.dto.NotificationResponse;
import com.example.drmayday.features.notification.dto.SetDevTokenRequest;
import com.example.drmayday.features.notification.service.NotificationService;
import com.example.drmayday.repository.NotificationRepository;
import com.example.drmayday.repository.UserFcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int FCM_TOKEN_MAX_LENGTH = 512;
    private static final int DEVICE_TYPE_MAX_LENGTH = 50;

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public FcmTokenResponse registerDeviceToken(SetDevTokenRequest request) {
        if (request == null || request.getFcmToken() == null || request.getFcmToken().isBlank()) {
            throw new AppException(ErrorCode.FCM_TOKEN_REQUIRED);
        }
        String fcmToken = request.getFcmToken().trim();
        if (fcmToken.length() > FCM_TOKEN_MAX_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "fcm_token không được vượt quá 512 ký tự");
        }

        if (request.getDeviceType() == null || request.getDeviceType().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }
        String deviceType = request.getDeviceType().trim().toLowerCase(Locale.ROOT);
        if (deviceType.length() > DEVICE_TYPE_MAX_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "device_type không được vượt quá 50 ký tự");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();

        UserFcmToken entity = userFcmTokenRepository.findByFcmToken(fcmToken)
                .map(existing -> {
                    existing.setUserId(currentUserId);
                    existing.setDeviceType(deviceType);
                    existing.setUpdatedAt(now);
                    return existing;
                })
                .orElseGet(() -> UserFcmToken.builder()
                        .userId(currentUserId)
                        .fcmToken(fcmToken)
                        .deviceType(deviceType)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        UserFcmToken saved = userFcmTokenRepository.saveAndFlush(entity);
        return toFcmTokenResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String pageStr, String limitStr) {
        int page = parsePositiveInt(pageStr, DEFAULT_PAGE, "page");
        int limit = parsePositiveInt(limitStr, DEFAULT_LIMIT, "limit");
        if (limit > MAX_LIMIT) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "limit không được vượt quá 100");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        return notificationRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(currentUserId, PageRequest.of(page - 1, limit))
                .getContent()
                .stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String id) {
        Long notificationId = parseNotificationId(id);
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Notification notification = notificationRepository.findByIdAndUserId(notificationId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.setRead(true);
        notification.setUpdatedAt(OffsetDateTime.now());
        Notification saved = notificationRepository.saveAndFlush(notification);
        return toNotificationResponse(saved);
    }

    @Override
    @Transactional
    public void deleteNotification(String id) {
        Long notificationId = parseNotificationId(id);
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Notification notification = notificationRepository.findByIdAndUserId(notificationId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notificationRepository.delete(notification);
        notificationRepository.flush();
    }

    private int parsePositiveInt(String raw, int defaultValue, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_TYPE,
                    fieldName + " phải là số nguyên hợp lệ");
        }
        if (parsed < 1) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    fieldName + " phải lớn hơn hoặc bằng 1");
        }
        return parsed;
    }

    private Long parseNotificationId(String id) {
        if (id == null || id.isBlank()) {
            throw new AppException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    private FcmTokenResponse toFcmTokenResponse(UserFcmToken token) {
        return FcmTokenResponse.builder()
                .id(String.valueOf(token.getId()))
                .userId(String.valueOf(token.getUserId()))
                .fcmToken(token.getFcmToken())
                .deviceType(token.getDeviceType())
                .createdAt(token.getCreatedAt())
                .updatedAt(token.getUpdatedAt())
                .build();
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(String.valueOf(notification.getId()))
                .userId(String.valueOf(notification.getUserId()))
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .avatar(notification.getAvatar())
                .referenceId(notification.getReferenceId())
                .isRead(Boolean.TRUE.equals(notification.getRead()) ? "1" : "0")
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
