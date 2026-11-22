package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.*;
import com.example.drmayday.features.medicalrecord.dto.AddLesionDetailsBulkRequest;
import com.example.drmayday.features.medicalrecord.dto.LesionDetailItemRequest;
import com.example.drmayday.features.medicalrecord.dto.LesionDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateLesionDetailRequest;
import com.example.drmayday.features.medicalrecord.service.LesionDetailsService;
import com.example.drmayday.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LesionDetailsServiceImpl implements LesionDetailsService {

    private final MedicalVisitRepository medicalVisitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AcuteLesionDetailRepository acuteLesionDetailRepository;
    private final WhealLesionDetailRepository whealLesionDetailRepository;
    private final WhealFeatureRepository whealFeatureRepository;
    private final UserAccountRepository userAccountRepository;

    public LesionDetailsServiceImpl(MedicalVisitRepository medicalVisitRepository,
                                    MedicalRecordRepository medicalRecordRepository,
                                    AcuteLesionDetailRepository acuteLesionDetailRepository,
                                    WhealLesionDetailRepository whealLesionDetailRepository,
                                    WhealFeatureRepository whealFeatureRepository,
                                    UserAccountRepository userAccountRepository) {
        this.medicalVisitRepository = medicalVisitRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.acuteLesionDetailRepository = acuteLesionDetailRepository;
        this.whealLesionDetailRepository = whealLesionDetailRepository;
        this.whealFeatureRepository = whealFeatureRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional
    public List<LesionDetailResponse> addLesionDetailsBulk(AddLesionDetailsBulkRequest req) {
        if (req == null || req.getExamId() == null || req.getExamId().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: exam_id");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: items không thể để trống");
        }

        Long examIdLong;
        try {
            examIdLong = Long.parseLong(req.getExamId().trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Tham số exam_id phải là giá trị số");
        }

        MedicalVisit visit = medicalVisitRepository.findById(examIdLong)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND, "Không tìm thấy phiên khám: " + examIdLong));

        String loaiBenhAn = visit.getMedicalRecordType() != null ? visit.getMedicalRecordType().toUpperCase() : "CAP";
        List<LesionDetailResponse> result = new ArrayList<>();

        if ("CAP".equals(loaiBenhAn)) {
            for (LesionDetailItemRequest item : req.getItems()) {
                String viTri = item.getViTriTonThuong() != null ? item.getViTriTonThuong() : item.getViTri();
                if (viTri == null || viTri.isBlank()) {
                    throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Vị trí tổn thương là bắt buộc cho chi tiết tổn thương cấp tính");
                }
                if (item.getKichThuocMm() != null && item.getKichThuocMm().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Kích thước mm phải lớn hơn 0");
                }
                if (item.getThoiGianTonTaiGio() != null && item.getThoiGianTonTaiGio().compareTo(BigDecimal.ZERO) < 0) {
                    throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Thời gian tồn tại giờ phải lớn hơn hoặc bằng 0");
                }

                AcuteLesionDetail entity = AcuteLesionDetail.builder()
                        .medicalVisitId(examIdLong)
                        .location(viTri)
                        .lesionType(item.getLoaiTonThuong())
                        .maximumSizeMm(item.getKichThuocMm())
                        .uniformSizeMm(item.getKichThuocMm())
                        .averageLesionDuration(item.getThoiGianTonTaiGio())
                        .specialLocationDescription(item.getMoTaChiTiet())
                        .otherLesionType(item.getMauSac())
                        .build();

                AcuteLesionDetail saved = acuteLesionDetailRepository.save(entity);
                result.add(mapAcuteToResponse(saved));
            }
        } else if ("MAN".equals(loaiBenhAn)) {
            for (LesionDetailItemRequest item : req.getItems()) {
                if (item.getMaDacDiemSanPhu() == null || item.getMaDacDiemSanPhu().isBlank()) {
                    throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Mã đặc điểm sẩn phù là bắt buộc cho chi tiết tổn thương mạn tính");
                }
                if (item.getGiaTriGhiNhan() == null || item.getGiaTriGhiNhan().isBlank()) {
                    throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Giá trị ghi nhận là bắt buộc cho chi tiết tổn thương mạn tính");
                }

                Long featureId;
                try {
                    featureId = Long.parseLong(item.getMaDacDiemSanPhu().trim());
                } catch (NumberFormatException e) {
                    throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Mã đặc điểm sẩn phù phải là giá trị số");
                }

                WhealFeature feature = whealFeatureRepository.findById(featureId)
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Không tìm thấy mã đặc điểm sẩn phù: " + featureId));

                WhealLesionDetail entity = WhealLesionDetail.builder()
                        .medicalVisitId(examIdLong)
                        .whealFeatureId(featureId)
                        .lesionType(item.getGiaTriGhiNhan())
                        .location(item.getViTriGhiNhan())
                        .build();

                WhealLesionDetail saved = whealLesionDetailRepository.save(entity);
                result.add(mapWhealToResponse(saved, feature.getName(), item.getGhiChu()));
            }
        } else {
            throw new AppException(ErrorCode.RECORD_TYPE_MISMATCH, "Loại bệnh án không được hỗ trợ: " + loaiBenhAn);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LesionDetailResponse> getListLesionDetails(String examId) {
        if (examId == null || examId.isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: exam_id");
        }

        Long examIdLong;
        try {
            examIdLong = Long.parseLong(examId.trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Tham số exam_id phải là giá trị số");
        }

        MedicalVisit visit = medicalVisitRepository.findById(examIdLong)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND, "Không tìm thấy phiên khám: " + examIdLong));

        // Verify ownership when the caller is a patient.
        if (SecurityUtils.hasRole("patient") && !SecurityUtils.hasRole("doctor") && !SecurityUtils.hasRole("admin")) {
            MedicalRecord record = medicalRecordRepository.findById(visit.getMedicalRecordId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hồ sơ bệnh án: " + visit.getMedicalRecordId()));

            Long currentUserId = SecurityUtils.getCurrentUserId();
            UserAccount account = userAccountRepository.findById(currentUserId).orElse(null);
            Long callerPatientId = (account != null && account.getPerson() != null)
                    ? account.getPerson().getPersonId()
                    : currentUserId;

            if (!record.getPatientId().equals(callerPatientId)) {
                throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, "Người dùng không có quyền truy cập dữ liệu của người khác");
            }
        }

        String loaiBenhAn = visit.getMedicalRecordType() != null ? visit.getMedicalRecordType().toUpperCase() : "CAP";

        if ("CAP".equals(loaiBenhAn)) {
            List<AcuteLesionDetail> list = acuteLesionDetailRepository.findByMedicalVisitId(examIdLong);
            return list.stream().map(this::mapAcuteToResponse).collect(Collectors.toList());
        } else {
            List<WhealLesionDetail> list = whealLesionDetailRepository.findByMedicalVisitId(examIdLong);
            Map<Long, String> featureNames = new java.util.HashMap<>();
            for (WhealFeature feature : whealFeatureRepository.findAll()) {
                if (feature != null && feature.getWhealFeatureId() != null) {
                    featureNames.put(feature.getWhealFeatureId(), feature.getName());
                }
            }

            return list.stream().map(w -> {
                String featureName = w.getWhealFeatureId() != null ? featureNames.get(w.getWhealFeatureId()) : null;
                return mapWhealToResponse(w, featureName, null);
            }).collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public LesionDetailResponse updateLesionDetail(String id, String loaiBenhAn, UpdateLesionDetailRequest req) {
        if (loaiBenhAn == null || loaiBenhAn.isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: loại bệnh án");
        }

        String normalizedType = loaiBenhAn.trim().toUpperCase();
        if (!"CAP".equals(normalizedType) && !"MAN".equals(normalizedType)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Loại bệnh án phải là CAP hoặc MAN");
        }

        Long idLong;
        try {
            idLong = Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Tham số id phải là giá trị số");
        }

        if ("CAP".equals(normalizedType)) {
            AcuteLesionDetail entity = acuteLesionDetailRepository.findById(idLong)
                    .orElseThrow(() -> new AppException(ErrorCode.LESION_DETAIL_NOT_FOUND, "Không tìm thấy chi tiết tổn thương với id: " + idLong));

            if (req.getKichThuocMm() != null && req.getKichThuocMm().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Kích thước mm phải lớn hơn 0");
            }
            if (req.getThoiGianTonTaiGio() != null && req.getThoiGianTonTaiGio().compareTo(BigDecimal.ZERO) < 0) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Thời gian tồn tại (giờ) phải lớn hơn hoặc bằng 0");
            }

            String viTri = req.getViTriTonThuong() != null ? req.getViTriTonThuong() : req.getViTri();
            if (viTri != null) entity.setLocation(viTri);
            if (req.getLoaiTonThuong() != null) entity.setLesionType(req.getLoaiTonThuong());
            if (req.getKichThuocMm() != null) {
                entity.setMaximumSizeMm(req.getKichThuocMm());
                entity.setUniformSizeMm(req.getKichThuocMm());
            }
            if (req.getMauSac() != null) entity.setOtherLesionType(req.getMauSac());
            if (req.getThoiGianTonTaiGio() != null) entity.setAverageLesionDuration(req.getThoiGianTonTaiGio());
            if (req.getMoTaChiTiet() != null) entity.setSpecialLocationDescription(req.getMoTaChiTiet());

            AcuteLesionDetail saved = acuteLesionDetailRepository.save(entity);
            return mapAcuteToResponse(saved);
        } else {
            WhealLesionDetail entity = whealLesionDetailRepository.findById(idLong)
                    .orElseThrow(() -> new AppException(ErrorCode.LESION_DETAIL_NOT_FOUND, "Không tìm thấy chi tiết tổn thương với id: " + idLong));

            String featureName = null;
            if (req.getMaDacDiemSanPhu() != null && !req.getMaDacDiemSanPhu().isBlank()) {
                Long featureId;
                try {
                    featureId = Long.parseLong(req.getMaDacDiemSanPhu().trim());
                } catch (NumberFormatException e) {
                    throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Mã đặc điểm sẩn phù phải là giá trị số");
                }
                WhealFeature feature = whealFeatureRepository.findById(featureId)
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Không tìm thấy mã đặc điểm sẩn phù: " + featureId));
                entity.setWhealFeatureId(featureId);
                featureName = feature.getName();
            } else if (entity.getWhealFeatureId() != null) {
                featureName = whealFeatureRepository.findById(entity.getWhealFeatureId())
                        .map(feature -> feature != null ? feature.getName() : null).orElse(null);
            }

            if (req.getGiaTriGhiNhan() != null) entity.setLesionType(req.getGiaTriGhiNhan());
            if (req.getViTriGhiNhan() != null) entity.setLocation(req.getViTriGhiNhan());

            WhealLesionDetail saved = whealLesionDetailRepository.save(entity);
            return mapWhealToResponse(saved, featureName, req.getGhiChu());
        }
    }

    @Override
    @Transactional
    public void deleteLesionDetail(String id, String loaiBenhAn) {
        if (loaiBenhAn == null || loaiBenhAn.isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: loai_benh_an");
        }

        String normalizedType = loaiBenhAn.trim().toUpperCase();
        if (!"CAP".equals(normalizedType) && !"MAN".equals(normalizedType)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Loại bệnh án phải là cấp tính hoặc mạn tính");
        }

        Long idLong;
        try {
            idLong = Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "id phải là giá trị số");
        }

        if ("CAP".equals(normalizedType)) {
            if (!acuteLesionDetailRepository.existsById(idLong)) {
                throw new AppException(ErrorCode.LESION_DETAIL_NOT_FOUND, "Không tìm thấy chi tiết tổn thương với id: " + idLong);
            }
            acuteLesionDetailRepository.deleteById(idLong);
        } else {
            if (!whealLesionDetailRepository.existsById(idLong)) {
                throw new AppException(ErrorCode.LESION_DETAIL_NOT_FOUND, "Không tìm thấy chi tiết tổn thương với id: " + idLong);
            }
            whealLesionDetailRepository.deleteById(idLong);
        }
    }

    private LesionDetailResponse mapAcuteToResponse(AcuteLesionDetail entity) {
        return LesionDetailResponse.builder()
                .lesionDetailId(entity.getAcuteLesionDetailId() != null ? String.valueOf(entity.getAcuteLesionDetailId()) : null)
                .examId(entity.getMedicalVisitId() != null ? String.valueOf(entity.getMedicalVisitId()) : null)
                .loaiBenhAn("CAP")
                .viTriTonThuong(entity.getLocation())
                .loaiTonThuong(entity.getLesionType())
                .kichThuocMm(entity.getMaximumSizeMm() != null ? entity.getMaximumSizeMm() : entity.getUniformSizeMm())
                .mauSac(entity.getOtherLesionType())
                .thoiGianTonTaiGio(entity.getAverageLesionDuration())
                .moTaChiTiet(entity.getSpecialLocationDescription())
                .build();
    }

    private LesionDetailResponse mapWhealToResponse(WhealLesionDetail entity, String featureName, String ghiChu) {
        return LesionDetailResponse.builder()
                .lesionDetailId(entity.getWhealLesionDetailId() != null ? String.valueOf(entity.getWhealLesionDetailId()) : null)
                .examId(entity.getMedicalVisitId() != null ? String.valueOf(entity.getMedicalVisitId()) : null)
                .loaiBenhAn("MAN")
                .maDacDiemSanPhu(entity.getWhealFeatureId() != null ? String.valueOf(entity.getWhealFeatureId()) : null)
                .tenDacDiem(featureName)
                .giaTriGhiNhan(entity.getLesionType())
                .viTriGhiNhan(entity.getLocation())
                .ghiChu(ghiChu)
                .build();
    }
}
