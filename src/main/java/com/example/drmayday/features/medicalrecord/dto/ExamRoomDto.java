package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.ClinicRoom;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Phòng của phiên khám (API 47, 48), null khi phiên chưa có phòng.
 */
public record ExamRoomDto(
        @JsonProperty("ma_phong_kham") String clinicRoomId,
        @JsonProperty("ten_phong_kham") String name) {

    public static ExamRoomDto from(ClinicRoom room) {
        return room != null ? new ExamRoomDto(JsonValues.id(room.getClinicRoomId()), room.getName()) : null;
    }
}
