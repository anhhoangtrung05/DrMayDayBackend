package com.example.drmayday.features.medicalrecord;

import com.example.drmayday.features.medicalrecord.dto.JsonValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Đồng hồ theo giờ phòng khám. "Hôm nay" của phiên khám tính theo Asia/Ho_Chi_Minh, không theo giờ máy chủ.
 * Test thay bean này bằng Clock cố định để kiểm tra biên ngày.
 */
@Configuration
public class ClinicClockConfig {

    @Bean
    public Clock clinicClock() {
        return Clock.system(JsonValues.CLINIC_ZONE);
    }
}
