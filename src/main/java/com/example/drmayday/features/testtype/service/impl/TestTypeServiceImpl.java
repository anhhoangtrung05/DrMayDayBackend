package com.example.drmayday.features.testtype.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.ProvocationTestType;
import com.example.drmayday.features.testtype.dto.CreateTestTypeRequest;
import com.example.drmayday.features.testtype.dto.TestTypeResponse;
import com.example.drmayday.features.testtype.dto.UpdateTestTypeRequest;
import com.example.drmayday.features.testtype.service.TestTypeService;
import com.example.drmayday.repository.ProvocationTestResultRepository;
import com.example.drmayday.repository.ProvocationTestTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestTypeServiceImpl implements TestTypeService {

    private static final int TEST_NAME_MAX_LENGTH = 150;

    private final ProvocationTestTypeRepository testTypeRepository;
    private final ProvocationTestResultRepository testResultRepository;

    @Override
    @Transactional
    public TestTypeResponse createTestType(CreateTestTypeRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        String normalizedName = validateTestName(request.getTenNghiemPhap());
        validateStandardDuration(request.getThoiGianChuan());

        if (testTypeRepository.existsByTestName(normalizedName)) {
            throw new AppException(ErrorCode.TEST_TYPE_ALREADY_EXISTS);
        }

        OffsetDateTime now = OffsetDateTime.now();
        ProvocationTestType entity = ProvocationTestType.builder()
                .testName(normalizedName)
                .implementationMethod(trimToNull(request.getPhuongPhapThucHien()))
                .standardDuration(toBigDecimal(request.getThoiGianChuan()))
                .otherCause(Boolean.TRUE.equals(request.getLaCanNguyenKhac()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            return toResponse(testTypeRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            if (containsConstraint(ex, "uq_loai_nghiem_phap_ten")) {
                throw new AppException(ErrorCode.TEST_TYPE_ALREADY_EXISTS);
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestTypeResponse> getTestTypes() {
        return testTypeRepository.findAllByOrderByProvocationTestIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TestTypeResponse getTestType(Long id) {
        ProvocationTestType entity = testTypeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_TYPE_NOT_FOUND));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public TestTypeResponse updateTestType(Long id, UpdateTestTypeRequest request) {
        ProvocationTestType entity = testTypeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_TYPE_NOT_FOUND));

        if (request == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        String normalizedName = validateTestName(request.getTenNghiemPhap());
        validateStandardDuration(request.getThoiGianChuan());

        if (testTypeRepository.existsByTestNameAndProvocationTestIdNot(normalizedName, id)) {
            throw new AppException(ErrorCode.TEST_TYPE_ALREADY_EXISTS);
        }

        if (request.getLaCanNguyenKhac() != null
                && !request.getLaCanNguyenKhac().equals(Boolean.TRUE.equals(entity.getOtherCause()))
                && testResultRepository.existsByProvocationTestTypeId(id)) {
            throw new AppException(ErrorCode.TEST_TYPE_IN_USE,
                    "Không thể thay đổi la_can_nguyen_khac vì loại nghiệm pháp đã có kết quả tham chiếu");
        }

        entity.setTestName(normalizedName);
        entity.setImplementationMethod(trimToNull(request.getPhuongPhapThucHien()));
        entity.setStandardDuration(toBigDecimal(request.getThoiGianChuan()));
        if (request.getLaCanNguyenKhac() != null) {
            entity.setOtherCause(request.getLaCanNguyenKhac());
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (entity.getCreatedAt() != null && now.isBefore(entity.getCreatedAt())) {
            now = entity.getCreatedAt();
        }
        entity.setUpdatedAt(now);

        try {
            return toResponse(testTypeRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            if (containsConstraint(ex, "uq_loai_nghiem_phap_ten")) {
                throw new AppException(ErrorCode.TEST_TYPE_ALREADY_EXISTS);
            }
            if (containsConstraint(ex, "fk_kqnp_phanloai") || containsConstraint(ex, "fk_chi_tiet_kqnp_loai")) {
                throw new AppException(ErrorCode.TEST_TYPE_IN_USE);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteTestType(Long id) {
        ProvocationTestType entity = testTypeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_TYPE_NOT_FOUND));

        if (testResultRepository.existsByProvocationTestTypeId(id)) {
            throw new AppException(ErrorCode.TEST_TYPE_IN_USE);
        }

        try {
            testTypeRepository.delete(entity);
            testTypeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (containsConstraint(ex, "fk_chi_tiet_kqnp_loai") || containsConstraint(ex, "fk_kqnp_phanloai")) {
                throw new AppException(ErrorCode.TEST_TYPE_IN_USE);
            }
            throw ex;
        }
    }

    private String validateTestName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }
        String trimmed = rawName.trim();
        if (trimmed.length() > TEST_NAME_MAX_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "ten_nghiem_phap không được vượt quá 150 ký tự");
        }
        return trimmed;
    }

    private void validateStandardDuration(Integer thoiGianChuan) {
        if (thoiGianChuan != null && thoiGianChuan <= 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "thoi_gian_chuan phải lớn hơn 0");
        }
    }

    private BigDecimal toBigDecimal(Integer value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean containsConstraint(Throwable ex, String constraintName) {
        for (Throwable cur = ex; cur != null; cur = cur.getCause()) {
            String msg = cur.getMessage();
            if (msg != null && msg.toLowerCase().contains(constraintName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private TestTypeResponse toResponse(ProvocationTestType entity) {
        return TestTypeResponse.builder()
                .maNghiemPhap(entity.getProvocationTestId())
                .tenNghiemPhap(entity.getTestName())
                .phuongPhapThucHien(entity.getImplementationMethod())
                .thoiGianChuan(entity.getStandardDuration() != null ? entity.getStandardDuration().intValue() : null)
                .laCanNguyenKhac(Boolean.TRUE.equals(entity.getOtherCause()))
                .build();
    }
}
