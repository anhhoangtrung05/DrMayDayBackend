package com.example.drmayday.features.notification.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.Notification;
import com.example.drmayday.entity.UserFcmToken;
import com.example.drmayday.features.notification.dto.FcmTokenResponse;
import com.example.drmayday.features.notification.dto.NotificationResponse;
import com.example.drmayday.features.notification.dto.SetDevTokenRequest;
import com.example.drmayday.repository.NotificationRepository;
import com.example.drmayday.repository.UserFcmTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private UserFcmTokenRepository userFcmTokenRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final Long CURRENT_USER_ID = 2L;

    @BeforeEach
    void setUpAuth() {
        UserPrincipal principal = UserPrincipal.create(CURRENT_USER_ID, "0912345678", "benh_nhan");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("API 100: registerDeviceToken")
    class RegisterDeviceTokenTests {

        @Test
        @DisplayName("[Happy Path] Registers a new FCM token successfully")
        void registerDeviceToken_new_success() {
            OffsetDateTime now = OffsetDateTime.now();
            when(userFcmTokenRepository.findByFcmToken("token-abc-123")).thenReturn(Optional.empty());
            when(userFcmTokenRepository.saveAndFlush(any(UserFcmToken.class))).thenAnswer(inv -> {
                UserFcmToken saved = inv.getArgument(0);
                saved.setId(10L);
                saved.setCreatedAt(now);
                saved.setUpdatedAt(now);
                return saved;
            });

            FcmTokenResponse res = notificationService.registerDeviceToken(
                    SetDevTokenRequest.builder().fcmToken("  token-abc-123  ").deviceType("Android").build());

            assertEquals("10", res.getId());
            assertEquals("2", res.getUserId());
            assertEquals("token-abc-123", res.getFcmToken());
            assertEquals("android", res.getDeviceType());
            assertNotNull(res.getCreatedAt());
            assertNotNull(res.getUpdatedAt());
        }

        @Test
        @DisplayName("[Happy Path] Upserts existing FCM token when re-registered or switched user")
        void registerDeviceToken_existing_updatesUserAndDevice() {
            OffsetDateTime created = OffsetDateTime.now().minusDays(1);
            UserFcmToken existing = UserFcmToken.builder()
                    .id(15L)
                    .userId(99L)
                    .fcmToken("shared-device-token")
                    .deviceType("ios")
                    .createdAt(created)
                    .updatedAt(created)
                    .build();

            when(userFcmTokenRepository.findByFcmToken("shared-device-token")).thenReturn(Optional.of(existing));
            when(userFcmTokenRepository.saveAndFlush(any(UserFcmToken.class))).thenAnswer(inv -> inv.getArgument(0));

            FcmTokenResponse res = notificationService.registerDeviceToken(
                    SetDevTokenRequest.builder().fcmToken("shared-device-token").deviceType("web").build());

            assertEquals("15", res.getId());
            assertEquals("2", res.getUserId());
            assertEquals("web", res.getDeviceType());
            assertEquals(created, res.getCreatedAt());
            assertTrue(res.getUpdatedAt().isAfter(created) || res.getUpdatedAt().isEqual(created));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing or blank fcm_token throws 9101")
        void registerDeviceToken_blankFcmToken_throws9101() {
            AppException ex1 = assertThrows(AppException.class, () ->
                    notificationService.registerDeviceToken(null));
            assertEquals(ErrorCode.FCM_TOKEN_REQUIRED, ex1.getErrorCode());

            AppException ex2 = assertThrows(AppException.class, () ->
                    notificationService.registerDeviceToken(SetDevTokenRequest.builder()
                            .fcmToken("   ").deviceType("android").build()));
            assertEquals(ErrorCode.FCM_TOKEN_REQUIRED, ex2.getErrorCode());
        }

        @Test
        @DisplayName("[Unhappy Path] Missing device_type throws 2001")
        void registerDeviceToken_blankDeviceType_throws2001() {
            AppException ex = assertThrows(AppException.class, () ->
                    notificationService.registerDeviceToken(SetDevTokenRequest.builder()
                            .fcmToken("token-xyz").deviceType(" ").build()));
            assertEquals(ErrorCode.MISSING_REQUIRED_PARAMETER, ex.getErrorCode());
        }

        @Test
        @DisplayName("[Unhappy Path] Exceeding max lengths throws 2003")
        void registerDeviceToken_tooLong_throws2003() {
            AppException exToken = assertThrows(AppException.class, () ->
                    notificationService.registerDeviceToken(SetDevTokenRequest.builder()
                            .fcmToken("a".repeat(513)).deviceType("android").build()));
            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exToken.getErrorCode());

            AppException exDevice = assertThrows(AppException.class, () ->
                    notificationService.registerDeviceToken(SetDevTokenRequest.builder()
                            .fcmToken("valid-token").deviceType("d".repeat(51)).build()));
            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exDevice.getErrorCode());
        }
    }

    @Nested
    @DisplayName("API 101: getMyNotifications")
    class GetMyNotificationsTests {

        @Test
        @DisplayName("[Happy Path] Uses default pagination and maps is_read as '0'/'1'")
        void getMyNotifications_defaults_success() {
            OffsetDateTime now = OffsetDateTime.now();
            Notification unread = Notification.builder()
                    .id(100L).userId(CURRENT_USER_ID).title("Nhắc lịch").body("Nội dung")
                    .type("appointment").avatar(null).referenceId("5").read(false)
                    .createdAt(now).updatedAt(now).build();
            Notification read = Notification.builder()
                    .id(99L).userId(CURRENT_USER_ID).title("Kết quả").body("Đã có KQ")
                    .type("lab_result").avatar("https://cdn/img.png").referenceId(null).read(true)
                    .createdAt(now.minusHours(1)).updatedAt(now).build();

            when(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                    eq(CURRENT_USER_ID), eq(PageRequest.of(0, 20))))
                    .thenReturn(new PageImpl<>(List.of(unread, read)));

            List<NotificationResponse> res = notificationService.getMyNotifications(null, null);

            assertEquals(2, res.size());
            assertEquals("100", res.get(0).getId());
            assertEquals("0", res.get(0).getIsRead());
            assertNull(res.get(0).getAvatar());
            assertEquals("1", res.get(1).getIsRead());
            assertEquals("https://cdn/img.png", res.get(1).getAvatar());
        }

        @Test
        @DisplayName("[Happy Path] Custom page & limit applies 0-based PageRequest")
        void getMyNotifications_customPagination() {
            when(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                    eq(CURRENT_USER_ID), eq(PageRequest.of(1, 5))))
                    .thenReturn(new PageImpl<>(List.of()));

            List<NotificationResponse> res = notificationService.getMyNotifications("2", "5");
            assertTrue(res.isEmpty());
            verify(notificationRepository).findByUserIdOrderByCreatedAtDescIdDesc(CURRENT_USER_ID, PageRequest.of(1, 5));
        }

        @Test
        @DisplayName("[Unhappy Path] Non-numeric or out-of-range page/limit throws 2002 or 2003")
        void getMyNotifications_invalidPagination() {
            AppException exType = assertThrows(AppException.class, () ->
                    notificationService.getMyNotifications("abc", "10"));
            assertEquals(ErrorCode.INVALID_PARAMETER_TYPE, exType.getErrorCode());

            AppException exPage = assertThrows(AppException.class, () ->
                    notificationService.getMyNotifications("0", "10"));
            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exPage.getErrorCode());

            AppException exLimit = assertThrows(AppException.class, () ->
                    notificationService.getMyNotifications("1", "101"));
            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exLimit.getErrorCode());
        }
    }

    @Nested
    @DisplayName("API 102: markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("[Happy Path] Marks own notification as read and returns is_read='1'")
        void markAsRead_success() {
            OffsetDateTime created = OffsetDateTime.now().minusMinutes(10);
            Notification entity = Notification.builder()
                    .id(12L).userId(CURRENT_USER_ID).title("Title").body("Body")
                    .type("general").read(false).createdAt(created).updatedAt(created).build();

            when(notificationRepository.findByIdAndUserId(12L, CURRENT_USER_ID)).thenReturn(Optional.of(entity));
            when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

            NotificationResponse res = notificationService.markAsRead("12");

            assertEquals("12", res.getId());
            assertEquals("1", res.getIsRead());
            assertTrue(entity.getRead());
        }

        @Test
        @DisplayName("[Unhappy Path] Missing, foreign, or invalid ID throws 9102")
        void markAsRead_notFoundOrForeign_throws9102() {
            when(notificationRepository.findByIdAndUserId(999L, CURRENT_USER_ID)).thenReturn(Optional.empty());

            AppException exNotFound = assertThrows(AppException.class, () ->
                    notificationService.markAsRead("999"));
            assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exNotFound.getErrorCode());

            AppException exBadId = assertThrows(AppException.class, () ->
                    notificationService.markAsRead("not-a-number"));
            assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exBadId.getErrorCode());
        }
    }

    @Nested
    @DisplayName("API 103: deleteNotification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("[Happy Path] Hard deletes own notification")
        void deleteNotification_success() {
            Notification entity = Notification.builder().id(12L).userId(CURRENT_USER_ID).build();
            when(notificationRepository.findByIdAndUserId(12L, CURRENT_USER_ID)).thenReturn(Optional.of(entity));

            notificationService.deleteNotification("12");

            verify(notificationRepository).delete(entity);
            verify(notificationRepository).flush();
        }

        @Test
        @DisplayName("[Unhappy Path] Missing or foreign notification throws 9102")
        void deleteNotification_notFound_throws9102() {
            when(notificationRepository.findByIdAndUserId(404L, CURRENT_USER_ID)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () ->
                    notificationService.deleteNotification("404"));
            assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
            verify(notificationRepository, never()).delete(any());
        }
    }
}
