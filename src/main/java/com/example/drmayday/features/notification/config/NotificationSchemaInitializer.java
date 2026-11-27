package com.example.drmayday.features.notification.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures operational notification tables exist outside the core clinical schema (sql/init.sql)
 * and seeds sample notifications when empty.
 */
@Component
@RequiredArgsConstructor
public class NotificationSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSchemaAndSampleData() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS public.user_fcm_tokens (" +
                    "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL REFERENCES public.taikhoan(ma_tai_khoan) ON DELETE CASCADE, " +
                    "fcm_token VARCHAR(512) NOT NULL UNIQUE, " +
                    "device_type VARCHAR(50) NOT NULL, " +
                    "created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            );
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_user_id ON public.user_fcm_tokens (user_id)"
            );

            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS public.notifications (" +
                    "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL REFERENCES public.taikhoan(ma_tai_khoan) ON DELETE CASCADE, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "body TEXT NOT NULL, " +
                    "type VARCHAR(50) NOT NULL DEFAULT 'general', " +
                    "avatar VARCHAR(500), " +
                    "reference_id VARCHAR(100), " +
                    "is_read BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            );
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON public.notifications (user_id, created_at DESC)"
            );

            seedSampleNotificationsIfEmpty();
        } catch (Exception ex) {
            log.warn("NotificationSchemaInitializer skipped or failed: {}", ex.getMessage());
        }
    }

    private void seedSampleNotificationsIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM public.notifications", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        List<Long> patientIds = jdbcTemplate.queryForList(
                "SELECT ma_tai_khoan FROM public.taikhoan WHERE ten_dang_nhap = '0912345678' LIMIT 1",
                Long.class);
        if (!patientIds.isEmpty()) {
            Long patientUserId = patientIds.get(0);
            jdbcTemplate.update(
                    "INSERT INTO public.notifications (user_id, title, body, type, avatar, reference_id, is_read) VALUES (?, ?, ?, ?, ?, ?, false)",
                    patientUserId,
                    "Nhắc nhở chấm điểm UAS7 hôm nay",
                    "Đừng quên ghi nhận điểm sẩn phù (HSS) và độ ngứa (ISS) hôm nay trong ứng dụng.",
                    "general",
                    null,
                    null
            );
            jdbcTemplate.update(
                    "INSERT INTO public.notifications (user_id, title, body, type, avatar, reference_id, is_read) VALUES (?, ?, ?, ?, ?, ?, false)",
                    patientUserId,
                    "Lịch tái khám sắp tới",
                    "Bạn có lịch hẹn kiểm tra định kỳ tại Phòng khám Đa khoa DrMayDay.",
                    "appointment",
                    null,
                    "1"
            );
        }

        List<Long> adminIds = jdbcTemplate.queryForList(
                "SELECT ma_tai_khoan FROM public.taikhoan WHERE ten_dang_nhap = '0900000001' LIMIT 1",
                Long.class);
        if (!adminIds.isEmpty()) {
            Long adminUserId = adminIds.get(0);
            jdbcTemplate.update(
                    "INSERT INTO public.notifications (user_id, title, body, type, avatar, reference_id, is_read) VALUES (?, ?, ?, ?, ?, ?, false)",
                    adminUserId,
                    "Thông báo hệ thống DrMayDay",
                    "Hệ thống thông báo đẩy FCM đã sẵn sàng hoạt động.",
                    "general",
                    null,
                    null
            );
        }
    }
}
