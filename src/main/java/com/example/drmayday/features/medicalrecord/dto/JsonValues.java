package com.example.drmayday.features.medicalrecord.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Định dạng giá trị trả về của phân hệ hồ sơ bệnh án, theo quy ước JSON của spec 0002.
 */
public final class JsonValues {

    public static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    public static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private JsonValues() {
    }

    public static String id(Long value) {
        return value != null ? String.valueOf(value) : null;
    }

    public static String timestamp(OffsetDateTime value) {
        return value != null ? value.atZoneSameInstant(CLINIC_ZONE).format(TIMESTAMP_FORMAT) : null;
    }

    public static String date(LocalDate value) {
        return value != null ? value.toString() : null;
    }

    public static String duration(Duration value) {
        return value != null ? value.toString() : null;
    }

    public static BigDecimal oneDecimal(BigDecimal value) {
        return value != null ? value.setScale(1, RoundingMode.HALF_UP) : null;
    }

    public static BigDecimal twoDecimals(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : null;
    }
}
