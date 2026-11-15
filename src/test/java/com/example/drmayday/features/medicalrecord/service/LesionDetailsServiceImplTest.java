package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.AcuteLesionDetail;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.entity.WhealFeature;
import com.example.drmayday.entity.WhealLesionDetail;
import com.example.drmayday.features.medicalrecord.dto.AddLesionDetailsBulkRequest;
import com.example.drmayday.features.medicalrecord.dto.LesionDetailItemRequest;
import com.example.drmayday.features.medicalrecord.dto.LesionDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateLesionDetailRequest;
import com.example.drmayday.features.medicalrecord.service.impl.LesionDetailsServiceImpl;
import com.example.drmayday.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LesionDetailsServiceImplTest {

    @Mock
    private MedicalVisitRepository medicalVisitRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private AcuteLesionDetailRepository acuteLesionDetailRepository;

    @Mock
    private WhealLesionDetailRepository whealLesionDetailRepository;

    @Mock
    private WhealFeatureRepository whealFeatureRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private LesionDetailsServiceImpl lesionDetailsService;

    @Test
    @DisplayName("API 54: Thêm bulk tổn thương CAP thành công")
    void addLesionDetailsBulk_cap_success() {
        MedicalVisit visit = MedicalVisit.builder()
                .medicalVisitId(1L)
                .medicalRecordType("CAP")
                .build();

        when(medicalVisitRepository.findById(1L)).thenReturn(Optional.of(visit));
        when(acuteLesionDetailRepository.save(any(AcuteLesionDetail.class))).thenAnswer(i -> {
            AcuteLesionDetail entity = i.getArgument(0);
            entity.setAcuteLesionDetailId(101L);
            return entity;
        });

        AddLesionDetailsBulkRequest req = AddLesionDetailsBulkRequest.builder()
                .examId("1")
                .items(List.of(
                        LesionDetailItemRequest.builder()
                                .viTriTonThuong("Chi trên")
                                .loaiTonThuong("San phu")
                                .kichThuocMm(BigDecimal.valueOf(20))
                                .build()
                ))
                .build();

        List<LesionDetailResponse> res = lesionDetailsService.addLesionDetailsBulk(req);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals("101", res.get(0).getLesionDetailId());
        assertEquals("CAP", res.get(0).getLoaiBenhAn());
        assertEquals("Chi trên", res.get(0).getViTriTonThuong());
        verify(acuteLesionDetailRepository).save(any(AcuteLesionDetail.class));
    }

    @Test
    @DisplayName("API 54: Thêm bulk tổn thương MAN thành công")
    void addLesionDetailsBulk_man_success() {
        MedicalVisit visit = MedicalVisit.builder()
                .medicalVisitId(2L)
                .medicalRecordType("MAN")
                .build();

        WhealFeature feature = WhealFeature.builder()
                .whealFeatureId(5L)
                .name("Hình tròn")
                .build();

        when(medicalVisitRepository.findById(2L)).thenReturn(Optional.of(visit));
        when(whealFeatureRepository.findById(5L)).thenReturn(Optional.of(feature));
        when(whealLesionDetailRepository.save(any(WhealLesionDetail.class))).thenAnswer(i -> {
            WhealLesionDetail entity = i.getArgument(0);
            entity.setWhealLesionDetailId(202L);
            return entity;
        });

        AddLesionDetailsBulkRequest req = AddLesionDetailsBulkRequest.builder()
                .examId("2")
                .items(List.of(
                        LesionDetailItemRequest.builder()
                                .maDacDiemSanPhu("5")
                                .giaTriGhiNhan("Nhẹ")
                                .viTriGhiNhan("Thân mình")
                                .build()
                ))
                .build();

        List<LesionDetailResponse> res = lesionDetailsService.addLesionDetailsBulk(req);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals("202", res.get(0).getLesionDetailId());
        assertEquals("MAN", res.get(0).getLoaiBenhAn());
        assertEquals("Hình tròn", res.get(0).getTenDacDiem());
        verify(whealLesionDetailRepository).save(any(WhealLesionDetail.class));
    }

    @Test
    @DisplayName("API 56: Cập nhật tổn thương CAP thành công")
    void updateLesionDetail_cap_success() {
        AcuteLesionDetail entity = AcuteLesionDetail.builder()
                .acuteLesionDetailId(101L)
                .medicalVisitId(1L)
                .location("Mặt")
                .build();

        when(acuteLesionDetailRepository.findById(101L)).thenReturn(Optional.of(entity));
        when(acuteLesionDetailRepository.save(any(AcuteLesionDetail.class))).thenAnswer(i -> i.getArgument(0));

        UpdateLesionDetailRequest req = UpdateLesionDetailRequest.builder()
                .viTriTonThuong("Toàn thân")
                .kichThuocMm(BigDecimal.valueOf(30))
                .build();

        LesionDetailResponse res = lesionDetailsService.updateLesionDetail("101", "CAP", req);

        assertNotNull(res);
        assertEquals("101", res.getLesionDetailId());
        assertEquals("Toàn thân", res.getViTriTonThuong());
        verify(acuteLesionDetailRepository).save(any(AcuteLesionDetail.class));
    }

    @Test
    @DisplayName("API 56: Cập nhật với ID không tồn tại quăng 7010")
    void updateLesionDetail_notFound() {
        when(acuteLesionDetailRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateLesionDetailRequest req = UpdateLesionDetailRequest.builder().build();

        AppException ex = assertThrows(AppException.class, () ->
                lesionDetailsService.updateLesionDetail("999", "CAP", req));
        assertEquals(ErrorCode.LESION_DETAIL_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("API 57: Xóa tổn thương CAP thành công")
    void deleteLesionDetail_success() {
        when(acuteLesionDetailRepository.existsById(101L)).thenReturn(true);
        doNothing().when(acuteLesionDetailRepository).deleteById(101L);

        assertDoesNotThrow(() -> lesionDetailsService.deleteLesionDetail("101", "CAP"));
        verify(acuteLesionDetailRepository).deleteById(101L);
    }

    @Test
    @DisplayName("API 57: Xóa với loai_benh_an không hợp lệ quăng 2003")
    void deleteLesionDetail_invalidType() {
        AppException ex = assertThrows(AppException.class, () ->
                lesionDetailsService.deleteLesionDetail("101", "UNKNOWN"));
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, ex.getErrorCode());
    }
}
