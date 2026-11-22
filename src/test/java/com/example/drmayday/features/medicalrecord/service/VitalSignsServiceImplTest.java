package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.VitalSign;
import com.example.drmayday.features.medicalrecord.dto.AddVitalSignsRequest;
import com.example.drmayday.features.medicalrecord.dto.VitalSignsResponse;
import com.example.drmayday.features.medicalrecord.service.impl.VitalSignsServiceImpl;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.repository.VitalSignRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalSignsServiceImplTest {

    @Mock
    private MedicalVisitRepository medicalVisitRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private VitalSignRepository vitalSignRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private VitalSignsServiceImpl vitalSignsService;

    @Test
    @DisplayName("API 52: Thêm dấu hiệu sinh tồn thành công")
    void addVitalSigns_success() {
        when(medicalVisitRepository.existsById(1L)).thenReturn(true);
        when(vitalSignRepository.save(any(VitalSign.class))).thenAnswer(i -> {
            VitalSign entity = i.getArgument(0);
            entity.setVitalSignId(50L);
            return entity;
        });

        AddVitalSignsRequest req = AddVitalSignsRequest.builder()
                .examId("1")
                .machLanPhut(80)
                .huyetApTamThu(120)
                .huyetApTamTruong(80)
                .nhipThoLanPhut(18)
                .spo2PhanTram(BigDecimal.valueOf(98.5))
                .build();

        VitalSignsResponse res = vitalSignsService.addVitalSigns(req);

        assertNotNull(res);
        assertEquals("50", res.getVitalSignId());
        assertEquals("1", res.getExamId());
        assertEquals(80, res.getMachLanPhut());
        assertEquals(120, res.getHuyetApTamThu());
        assertEquals(80, res.getHuyetApTamTruong());
        verify(vitalSignRepository).save(any(VitalSign.class));
    }

    @Test
    @DisplayName("API 52: Exam không tồn tại quăng 5008")
    void addVitalSigns_examNotFound() {
        when(medicalVisitRepository.existsById(99L)).thenReturn(false);

        AddVitalSignsRequest req = AddVitalSignsRequest.builder()
                .examId("99")
                .build();

        AppException ex = assertThrows(AppException.class, () -> vitalSignsService.addVitalSigns(req));
        assertEquals(ErrorCode.EXAM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("API 52: HA tâm trương >= tâm thu quăng 2003")
    void addVitalSigns_invalidBloodPressure() {
        when(medicalVisitRepository.existsById(1L)).thenReturn(true);

        AddVitalSignsRequest req = AddVitalSignsRequest.builder()
                .examId("1")
                .huyetApTamThu(110)
                .huyetApTamTruong(115)
                .build();

        AppException ex = assertThrows(AppException.class, () -> vitalSignsService.addVitalSigns(req));
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("API 52: SpO2 ngoài khoảng 0..100 quăng 2003")
    void addVitalSigns_invalidSpo2() {
        when(medicalVisitRepository.existsById(1L)).thenReturn(true);

        AddVitalSignsRequest req = AddVitalSignsRequest.builder()
                .examId("1")
                .spo2PhanTram(BigDecimal.valueOf(105))
                .build();

        AppException ex = assertThrows(AppException.class, () -> vitalSignsService.addVitalSigns(req));
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("API 52: co_bat_thuong_co_quan = true nhưng thiếu ghi rõ quăng 2003")
    void addVitalSigns_missingAbnormalityDescription() {
        when(medicalVisitRepository.existsById(1L)).thenReturn(true);

        AddVitalSignsRequest req = AddVitalSignsRequest.builder()
                .examId("1")
                .coBatThuongCoQuan(true)
                .batThuongCoQuanGhiRo(null)
                .build();

        AppException ex = assertThrows(AppException.class, () -> vitalSignsService.addVitalSigns(req));
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, ex.getErrorCode());
    }
}
