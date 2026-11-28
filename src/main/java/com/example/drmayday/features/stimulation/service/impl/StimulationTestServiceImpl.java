package com.example.drmayday.features.stimulation.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.ProvocationTestResult;
import com.example.drmayday.entity.ProvocationTestSession;
import com.example.drmayday.entity.ProvocationTestType;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.stimulation.dto.StimulationTestResultInput;
import com.example.drmayday.features.stimulation.dto.StimulationTestResultResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionDetailResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionResponse;
import com.example.drmayday.features.stimulation.dto.UpsertStimulationTestSessionRequest;
import com.example.drmayday.features.stimulation.service.StimulationTestService;
import com.example.drmayday.repository.ChronicVisitRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.ProvocationTestResultRepository;
import com.example.drmayday.repository.ProvocationTestSessionRepository;
import com.example.drmayday.repository.ProvocationTestTypeRepository;
import com.example.drmayday.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StimulationTestServiceImpl implements StimulationTestService {

    private static final String RECORD_TYPE_CHRONIC = "MAN";
    private static final Set<String> VALID_RESULTS = Set.of("Duong tinh", "Am tinh");
    private static final Set<String> NON_NEGATIVE_UNITS = Set.of("phut", "kg", "nm", "diem");
    private static final int UNIT_MAX_LENGTH = 30;
    private static final int NRS_MIN = 0;
    private static final int NRS_MAX = 10;

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalVisitRepository medicalVisitRepository;
    private final ChronicVisitRepository chronicVisitRepository;
    private final ProvocationTestTypeRepository testTypeRepository;
    private final ProvocationTestSessionRepository sessionRepository;
    private final ProvocationTestResultRepository resultRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public StimulationTestSessionResponse upsertSession(Long hoSoId, Long phienKhamId,
                                                        UpsertStimulationTestSessionRequest request) {
        validateChronicVisit(hoSoId, phienKhamId);

        if (request == null || request.getNgungKhangHistamine() == null || request.getNgungCorticoid() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        validateDiscontinuationPair(request.getNgungKhangHistamine(), request.getThoiGianNgungHistamine(), "histamine");
        validateDiscontinuationPair(request.getNgungCorticoid(), request.getThoiGianNgungCorticoid(), "corticoid");

        ProvocationTestSession session = sessionRepository.findByMedicalVisitId(phienKhamId)
                .orElseGet(() -> ProvocationTestSession.builder().medicalVisitId(phienKhamId).build());

        session.setStoppedAntihistamines(request.getNgungKhangHistamine());
        session.setHistamineDiscontinuationDays(toBigDecimal(request.getThoiGianNgungHistamine()));
        session.setStoppedCorticosteroids(request.getNgungCorticoid());
        session.setCorticosteroidDiscontinuationDays(toBigDecimal(request.getThoiGianNgungCorticoid()));
        session.setUpdatedAt(OffsetDateTime.now());

        ProvocationTestSession saved = sessionRepository.saveAndFlush(session);
        return toSessionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StimulationTestSessionDetailResponse getSessionDetail(Long hoSoId, Long phienKhamId) {
        MedicalRecord record = validateChronicVisit(hoSoId, phienKhamId);
        checkReadAccess(record);

        ProvocationTestSession session = sessionRepository.findByMedicalVisitId(phienKhamId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                        "Phiên khám chưa khởi tạo phiên nghiệm pháp kích thích"));

        List<ProvocationTestResult> results = resultRepository
                .findByProvocationTestSessionIdOrderByProvocationTestResultIdAsc(session.getProvocationTestSessionId());

        Set<Long> typeIds = results.stream()
                .map(ProvocationTestResult::getProvocationTestTypeId)
                .collect(Collectors.toSet());
        Map<Long, ProvocationTestType> typesById = testTypeRepository.findAllById(typeIds).stream()
                .collect(Collectors.toMap(ProvocationTestType::getProvocationTestId, t -> t));

        List<StimulationTestResultResponse> resultResponses = results.stream()
                .map(r -> toResultResponse(r, typesById.get(r.getProvocationTestTypeId())))
                .toList();

        return StimulationTestSessionDetailResponse.builder()
                .maPhienNghiemPhap(session.getProvocationTestSessionId())
                .maPhienKham(session.getMedicalVisitId())
                .ngungKhangHistamine(session.getStoppedAntihistamines())
                .thoiGianNgungHistamine(toInteger(session.getHistamineDiscontinuationDays()))
                .ngungCorticoid(session.getStoppedCorticosteroids())
                .thoiGianNgungCorticoid(toInteger(session.getCorticosteroidDiscontinuationDays()))
                .results(resultResponses)
                .build();
    }

    @Override
    @Transactional
    public List<StimulationTestResultResponse> createResultsBulk(Long hoSoId, Long phienKhamId,
                                                                 List<StimulationTestResultInput> requests) {
        validateChronicVisit(hoSoId, phienKhamId);

        ProvocationTestSession session = sessionRepository.findByMedicalVisitId(phienKhamId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                        "Phiên khám chưa khởi tạo phiên nghiệm pháp kích thích"));

        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        Map<Long, ProvocationTestType> typeCache = new HashMap<>();
        List<ProvocationTestResult> toSave = new ArrayList<>(requests.size());
        for (StimulationTestResultInput input : requests) {
            ProvocationTestResult entity = ProvocationTestResult.builder()
                    .provocationTestSessionId(session.getProvocationTestSessionId())
                    .build();
            applyAndValidateResult(entity, input, typeCache);
            toSave.add(entity);
        }

        List<ProvocationTestResult> saved = resultRepository.saveAllAndFlush(toSave);
        return saved.stream()
                .map(r -> toResultResponse(r, typeCache.get(r.getProvocationTestTypeId())))
                .toList();
    }

    @Override
    @Transactional
    public StimulationTestResultResponse updateResult(Long id, StimulationTestResultInput request) {
        ProvocationTestResult existing = resultRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.STIMULATION_TEST_RESULT_NOT_FOUND));

        Map<Long, ProvocationTestType> typeCache = new HashMap<>();
        ProvocationTestType testType = applyAndValidateResult(existing, request, typeCache);

        ProvocationTestResult saved = resultRepository.saveAndFlush(existing);
        return toResultResponse(saved, testType);
    }

    @Override
    @Transactional
    public void deleteResult(Long id) {
        ProvocationTestResult existing = resultRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.STIMULATION_TEST_RESULT_NOT_FOUND));
        resultRepository.delete(existing);
        resultRepository.flush();
    }

    private MedicalRecord validateChronicVisit(Long hoSoId, Long phienKhamId) {
        MedicalRecord record = medicalRecordRepository.findById(hoSoId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        MedicalVisit visit = medicalVisitRepository.findById(phienKhamId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        if (!hoSoId.equals(visit.getMedicalRecordId())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                    "Phiên khám không thuộc hồ sơ bệnh án được chỉ định");
        }

        if (!RECORD_TYPE_CHRONIC.equalsIgnoreCase(record.getMedicalRecordType())
                || !RECORD_TYPE_CHRONIC.equalsIgnoreCase(visit.getMedicalRecordType())
                || !chronicVisitRepository.existsById(phienKhamId)) {
            throw new AppException(ErrorCode.RECORD_TYPE_MISMATCH);
        }

        return record;
    }

    private void checkReadAccess(MedicalRecord record) {
        if (SecurityUtils.hasRole("ADMIN")
                || SecurityUtils.hasRole("BAC_SI")
                || SecurityUtils.hasRole("DOCTOR")
                || SecurityUtils.hasRole("DIEU_DUONG")
                || SecurityUtils.hasRole("NURSE")) {
            return;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long currentPersonId = userAccountRepository.findById(currentUserId)
                .map(UserAccount::getPerson)
                .map(Person::getPersonId)
                .orElse(currentUserId);
        if (!record.getPatientId().equals(currentPersonId)) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }
    }

    private void validateDiscontinuationPair(Boolean stopped, Integer days, String label) {
        if (Boolean.TRUE.equals(stopped)) {
            if (days == null) {
                throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
            }
            if (days < 0) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                        "Thời gian ngừng " + label + " không được âm");
            }
        } else if (days != null) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "Chỉ nhập thời gian ngừng " + label + " khi đã ngừng thuốc");
        }
    }

    private ProvocationTestType applyAndValidateResult(ProvocationTestResult target,
                                                       StimulationTestResultInput input,
                                                       Map<Long, ProvocationTestType> typeCache) {
        if (input == null
                || input.getMaNghiemPhap() == null
                || input.getLaCanNguyenKhac() == null
                || input.getKetQua() == null
                || input.getKetQua().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        String ketQua = input.getKetQua().trim();
        if (!VALID_RESULTS.contains(ketQua)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "ket_qua chỉ chấp nhận: Duong tinh hoặc Am tinh");
        }

        ProvocationTestType testType = typeCache.computeIfAbsent(
                input.getMaNghiemPhap(),
                id -> testTypeRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                                "ma_nghiem_phap không tồn tại trong danh mục"))
        );

        boolean otherCause = Boolean.TRUE.equals(input.getLaCanNguyenKhac());
        if (otherCause != Boolean.TRUE.equals(testType.getOtherCause())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "Cặp (ma_nghiem_phap, la_can_nguyen_khac) không khớp danh mục loại nghiệm pháp");
        }

        String otherCauseName = trimToNull(input.getCanNguyenKhacTen());
        if (otherCause && otherCauseName == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }
        if (!otherCause && otherCauseName != null) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "can_nguyen_khac_ten chỉ được phép nhập khi la_can_nguyen_khac = true");
        }

        BigDecimal numericThreshold = input.getNguongGiaTriSo();
        String textThreshold = trimToNull(input.getNguongGiaTriChu());
        if (numericThreshold != null && textThreshold != null) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "Không được truyền đồng thời cả nguong_gia_tri_so và nguong_gia_tri_chu");
        }

        String unit = trimToNull(input.getDonViDo());
        if ((numericThreshold != null || textThreshold != null) && unit == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }
        if (unit != null && unit.length() > UNIT_MAX_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "don_vi_do không được vượt quá 30 ký tự");
        }
        if (unit != null && NON_NEGATIVE_UNITS.contains(unit)
                && numericThreshold != null && numericThreshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "nguong_gia_tri_so không được âm với đơn vị " + unit);
        }

        validateNrsScore(input.getDiemNguaNrs());
        validateNrsScore(input.getDiemDauNrs());
        validateNrsScore(input.getDiemBongRatNrs());

        target.setProvocationTestTypeId(testType.getProvocationTestId());
        target.setOtherCause(otherCause);
        target.setOtherCauseName(otherCauseName);
        target.setTestResult(ketQua);
        target.setNumericThreshold(numericThreshold);
        target.setTextThreshold(textThreshold);
        target.setMeasurementUnit(unit);
        target.setItchNrsScore(toShort(input.getDiemNguaNrs()));
        target.setPainNrsScore(toShort(input.getDiemDauNrs()));
        target.setBurningNrsScore(toShort(input.getDiemBongRatNrs()));

        return testType;
    }

    private void validateNrsScore(Integer score) {
        if (score != null && (score < NRS_MIN || score > NRS_MAX)) {
            throw new AppException(ErrorCode.SYMPTOM_SCORE_OUT_OF_RANGE);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal toBigDecimal(Integer value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private Integer toInteger(BigDecimal value) {
        return value != null ? value.intValue() : null;
    }

    private Short toShort(Integer value) {
        return value != null ? value.shortValue() : null;
    }

    private Integer toInteger(Short value) {
        return value != null ? value.intValue() : null;
    }

    private StimulationTestSessionResponse toSessionResponse(ProvocationTestSession session) {
        return StimulationTestSessionResponse.builder()
                .maPhienNghiemPhap(session.getProvocationTestSessionId())
                .maPhienKham(session.getMedicalVisitId())
                .ngungKhangHistamine(session.getStoppedAntihistamines())
                .thoiGianNgungHistamine(toInteger(session.getHistamineDiscontinuationDays()))
                .ngungCorticoid(session.getStoppedCorticosteroids())
                .thoiGianNgungCorticoid(toInteger(session.getCorticosteroidDiscontinuationDays()))
                .build();
    }

    private StimulationTestResultResponse toResultResponse(ProvocationTestResult entity, ProvocationTestType testType) {
        return StimulationTestResultResponse.builder()
                .id(entity.getProvocationTestResultId())
                .maPhienNghiemPhap(entity.getProvocationTestSessionId())
                .maNghiemPhap(entity.getProvocationTestTypeId())
                .tenNghiemPhap(testType != null ? testType.getTestName() : null)
                .laCanNguyenKhac(Boolean.TRUE.equals(entity.getOtherCause()))
                .canNguyenKhacTen(entity.getOtherCauseName())
                .ketQua(entity.getTestResult())
                .nguongGiaTriSo(entity.getNumericThreshold())
                .nguongGiaTriChu(entity.getTextThreshold())
                .donViDo(entity.getMeasurementUnit())
                .diemNguaNrs(toInteger(entity.getItchNrsScore()))
                .diemDauNrs(toInteger(entity.getPainNrsScore()))
                .diemBongRatNrs(toInteger(entity.getBurningNrsScore()))
                .build();
    }
}
