package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.AcuteVisit;
import com.example.drmayday.entity.ChronicVisit;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.features.medicalrecord.dto.AcuteExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.ChronicExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAcuteExamFindingsRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertChronicExamFindingsRequest;
import com.example.drmayday.features.medicalrecord.service.impl.ExamFindingsServiceImpl;
import com.example.drmayday.repository.AcuteVisitRepository;
import com.example.drmayday.repository.ChronicVisitRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamFindingsServiceImplTest {

    @Mock
    private MedicalVisitRepository medicalVisitRepository;

    @Mock
    private AcuteVisitRepository acuteVisitRepository;

    @Mock
    private ChronicVisitRepository chronicVisitRepository;

    @InjectMocks
    private ExamFindingsServiceImpl examFindingsService;

    @Test
    @DisplayName("API 50: Upsert triệu chứng khám cấp thành công")
    void upsertAcuteExamFindings_success() {
        MedicalVisit visit = MedicalVisit.builder()
                .medicalVisitId(1L)
                .medicalRecordType("CAP")
                .build();

        when(medicalVisitRepository.findById(1L)).thenReturn(Optional.of(visit));
        when(acuteVisitRepository.findById(1L)).thenReturn(Optional.empty());
        when(acuteVisitRepository.save(any(AcuteVisit.class))).thenAnswer(i -> i.getArgument(0));

        UpsertAcuteExamFindingsRequest req = UpsertAcuteExamFindingsRequest.builder()
                .examId("1")
                .coSanPhu(true)
                .coAnhSanPhu(true)
                .trieuChungDaNiemMac(true)
                .soLanNon(2)
                .build();

        AcuteExamFindingsResponse res = examFindingsService.upsertAcuteExamFindings(req);

        assertNotNull(res);
        assertEquals("1", res.getExamId());
        assertEquals("CAP", res.getLoaiBenhAn());
        assertTrue(res.getCoSanPhu());
        assertEquals(2, res.getSoLanNon());
        verify(acuteVisitRepository).save(any(AcuteVisit.class));
    }

    @Test
    @DisplayName("API 50: Exam không tồn tại quăng 5008")
    void upsertAcuteExamFindings_notFound() {
        when(medicalVisitRepository.findById(99L)).thenReturn(Optional.empty());

        UpsertAcuteExamFindingsRequest req = UpsertAcuteExamFindingsRequest.builder()
                .examId("99")
                .build();

        AppException ex = assertThrows(AppException.class, () -> examFindingsService.upsertAcuteExamFindings(req));
        assertEquals(ErrorCode.EXAM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("API 50: Loại bệnh án sai (MAN thay vì CAP) quăng 4008")
    void upsertAcuteExamFindings_typeMismatch() {
        MedicalVisit visit = MedicalVisit.builder()
                .medicalVisitId(1L)
                .medicalRecordType("MAN")
                .build();

        when(medicalVisitRepository.findById(1L)).thenReturn(Optional.of(visit));

        UpsertAcuteExamFindingsRequest req = UpsertAcuteExamFindingsRequest.builder()
                .examId("1")
                .build();

        AppException ex = assertThrows(AppException.class, () -> examFindingsService.upsertAcuteExamFindings(req));
        assertEquals(ErrorCode.RECORD_EXAM_TYPE_MISMATCH, ex.getErrorCode());
    }

    @Test
    @DisplayName("API 50: so_lan_non âm quăng 2003")
    void upsertAcuteExamFindings_invalidSoLanNon() {
        MedicalVisit visit = MedicalVisit.builder()
                .medicalVisitId(1L)
                .medicalRecordType("CAP")
                .build();

        when(medicalVisitRepository.findById(1L)).thenReturn(Optional.of(visit));

        UpsertAcuteExamFindingsRequest req = UpsertAcuteExamFindingsRequest.builder()
                .examId("1")
                .soLanNon(-1)
                .build();

        AppException ex = assertThrows(AppException.class, () -> examFindingsService.upsertAcuteExamFindings(req));
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("API 51: Upsert triệu chứng khám mạn thành công")
    void upsertChronicExamFindings_success() {
        MedicalVisit visit = MedicalVisit.builder()
                .medicalVisitId(2L)
                .medicalRecordType("MAN")
                .build();

        when(medicalVisitRepository.findById(2L)).thenReturn(Optional.of(visit));
        when(chronicVisitRepository.findById(2L)).thenReturn(Optional.empty());
        when(chronicVisitRepository.save(any(ChronicVisit.class))).thenAnswer(i -> i.getArgument(0));

        UpsertChronicExamFindingsRequest req = UpsertChronicExamFindingsRequest.builder()
                .examId("2")
                .hienTaiCoSanPhu(true)
                .trongDotCoPhuMach(true)
                .diemAas7(45)
                .build();

        ChronicExamFindingsResponse res = examFindingsService.upsertChronicExamFindings(req);

        assertNotNull(res);
        assertEquals("2", res.getExamId());
        assertEquals("MAN", res.getLoaiBenhAn());
        assertEquals(45, res.getDiemAas7());
        verify(chronicVisitRepository).save(any(ChronicVisit.class));
    }

    @Test
    @DisplayName("API 51: diem_aas7 vượt quá 105 quăng 2003")
    void upsertChronicExamFindings_invalidAas7() {
        MedicalVisit visit = MedicalVisit.builder()
                .medicalVisitId(2L)
                .medicalRecordType("MAN")
                .build();

        when(medicalVisitRepository.findById(2L)).thenReturn(Optional.of(visit));

        UpsertChronicExamFindingsRequest req = UpsertChronicExamFindingsRequest.builder()
                .examId("2")
                .diemAas7(120)
                .build();

        AppException ex = assertThrows(AppException.class, () -> examFindingsService.upsertChronicExamFindings(req));
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("API 51: phu_vi_tri_khac = true nhưng thiếu ghi rõ quăng 2003")
    void upsertChronicExamFindings_missingGhiRo() {
        MedicalVisit visit = MedicalVisit.builder()
                .medicalVisitId(2L)
                .medicalRecordType("MAN")
                .build();

        when(medicalVisitRepository.findById(2L)).thenReturn(Optional.of(visit));

        UpsertChronicExamFindingsRequest req = UpsertChronicExamFindingsRequest.builder()
                .examId("2")
                .phuViTriKhac(true)
                .phuViTriKhacGhiRo("   ")
                .build();

        AppException ex = assertThrows(AppException.class, () -> examFindingsService.upsertChronicExamFindings(req));
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, ex.getErrorCode());
    }
}
