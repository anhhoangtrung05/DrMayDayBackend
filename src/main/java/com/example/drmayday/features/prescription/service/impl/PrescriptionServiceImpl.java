package com.example.drmayday.features.prescription.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.Medication;
import com.example.drmayday.entity.MedicationDiscontinuation;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.entity.Prescription;
import com.example.drmayday.entity.PrescriptionItem;
import com.example.drmayday.features.prescription.dto.CreatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.PrescriptionItemRequest;
import com.example.drmayday.features.prescription.dto.PrescriptionResponse;
import com.example.drmayday.features.prescription.dto.StopInstructionRequest;
import com.example.drmayday.features.prescription.dto.StopMedicationResponse;
import com.example.drmayday.features.prescription.dto.UpdatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.UpsertStopMedicationRequest;
import com.example.drmayday.features.prescription.service.PrescriptionService;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.MedicationDiscontinuationRepository;
import com.example.drmayday.repository.MedicationRepository;
import com.example.drmayday.repository.PrescriptionItemRepository;
import com.example.drmayday.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prescription APIs (PRESCRIPTIONS_*): prescriptions (donthuoc + chitietdonthuoc) and their stop-medication
 * instructions (chidanngungthuoc).
 */
@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    static final String STATUS_IN_TREATMENT = "DANG_DIEU_TRI";
    static final Set<String> VALID_STATUSES = Set.of(STATUS_IN_TREATMENT, "HOAN_THANH", "HUY");

    static final String PURPOSE_OTHER = "Khac";
    static final Set<String> VALID_STOP_PURPOSES = Set.of("Test huyet thanh tu than", "Test kich thich", PURPOSE_OTHER);

    // Column lengths from sql/init.sql.
    private static final int MEDICATION_ID_MAX = 50;
    private static final int ADDITIONAL_NAME_MAX = 255;
    private static final int DOSAGE_MAX = 100;
    private static final int DURATION_MAX = 100;
    private static final int TREATMENT_PLAN_ID_MAX = 50;
    private static final int MEDICATION_TO_STOP_MAX = 255;
    private static final int OTHER_PURPOSE_MAX = 255;

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationDiscontinuationRepository discontinuationRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalVisitRepository medicalVisitRepository;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   PrescriptionItemRepository prescriptionItemRepository,
                                   MedicationRepository medicationRepository,
                                   MedicationDiscontinuationRepository discontinuationRepository,
                                   MedicalRecordRepository medicalRecordRepository,
                                   MedicalVisitRepository medicalVisitRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.medicationRepository = medicationRepository;
        this.discontinuationRepository = discontinuationRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.medicalVisitRepository = medicalVisitRepository;
    }

    // ── PRESCRIPTIONS_CREATE_PRESCRIPTIONS ──

    @Override
    @Transactional
    public PrescriptionResponse createPrescription(CreatePrescriptionRequest req) {
        if (req == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu nội dung đơn thuốc");
        }
        requireId(req.getRecordId(), "record_id");
        requireId(req.getPhienKhamId(), "phien_kham_id");

        medicalRecordRepository.findById(req.getRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                        "Không tìm thấy hồ sơ bệnh án " + req.getRecordId()));
        medicalVisitRepository.findById(req.getPhienKhamId())
                .filter(visit -> req.getRecordId().equals(visit.getMedicalRecordId()))
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND,
                        "Không tìm thấy phiên khám " + req.getPhienKhamId()
                                + " thuộc hồ sơ bệnh án " + req.getRecordId()));

        String status = req.getTrangThaiDon() == null ? STATUS_IN_TREATMENT : req.getTrangThaiDon();
        validateStatus(status);
        validateItems(req.getItems());

        Prescription prescription = prescriptionRepository.save(Prescription.builder()
                .medicalRecordId(req.getRecordId())
                .medicalVisitId(req.getPhienKhamId())
                .prescriptionDate(req.getNgayKeDon() != null ? req.getNgayKeDon() : LocalDate.now())
                .prescriptionStatus(status)
                .treatmentNotes(req.getGhiChuDieuTri())
                .stopMedicationAtNextVisit(false)
                .build());

        List<PrescriptionItem> items = saveItems(prescription.getPrescriptionId(), req.getItems());
        return PrescriptionResponse.from(prescription, items);
    }

    // ── PRESCRIPTIONS_GET_PRESCRIPTIONS ──

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptions(Long examId, Long recordId) {
        if (examId == null && recordId == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    "Cần truyền ít nhất một trong hai tham số phien_kham_id hoặc record_id");
        }

        if (recordId != null) {
            checkPatientAccessToRecord(recordId);
        } else {
            medicalVisitRepository.findById(examId)
                    .ifPresent(visit -> checkPatientAccessToRecord(visit.getMedicalRecordId()));
        }

        List<Prescription> prescriptions = prescriptionRepository.findFiltered(examId, recordId);
        if (prescriptions.isEmpty()) {
            return List.of();
        }

        List<Long> ids = prescriptions.stream().map(Prescription::getPrescriptionId).toList();
        Map<Long, List<PrescriptionItem>> itemsByPrescription = prescriptionItemRepository
                .findByPrescriptionIdInOrderByPrescriptionItemIdAsc(ids).stream()
                .collect(Collectors.groupingBy(PrescriptionItem::getPrescriptionId));

        return prescriptions.stream()
                .map(p -> PrescriptionResponse.from(p, itemsByPrescription.getOrDefault(p.getPrescriptionId(), List.of())))
                .toList();
    }

    // ── PRESCRIPTIONS_UPDATE_PRESCRIPTION ──

    @Override
    @Transactional
    public PrescriptionResponse updatePrescription(Long prescriptionId, UpdatePrescriptionRequest req) {
        Prescription prescription = findPrescription(prescriptionId);
        if (req == null) {
            req = new UpdatePrescriptionRequest();
        }

        if (req.getTrangThaiDon() != null) {
            validateStatus(req.getTrangThaiDon());
            prescription.setPrescriptionStatus(req.getTrangThaiDon());
        }
        if (req.getGhiChuDieuTri() != null) {
            prescription.setTreatmentNotes(req.getGhiChuDieuTri());
        }

        List<PrescriptionItem> items;
        if (req.getItems() != null) {
            validateItems(req.getItems());
            prescriptionItemRepository.deleteByPrescriptionId(prescriptionId);
            prescriptionItemRepository.flush();
            items = saveItems(prescriptionId, req.getItems());
        } else {
            items = prescriptionItemRepository.findByPrescriptionIdOrderByPrescriptionItemIdAsc(prescriptionId);
        }

        prescription = prescriptionRepository.save(prescription);
        return PrescriptionResponse.from(prescription, items);
    }

    // ── PRESCRIPTIONS_UPSERT_STOP_MEDICATION ──

    @Override
    @Transactional
    public StopMedicationResponse upsertStopMedication(UpsertStopMedicationRequest req) {
        if (req == null || req.getDonThuocId() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: don_thuoc_id");
        }
        if (req.getInstructions() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    "Thiếu tham số bắt buộc: instructions (gửi mảng rỗng để xoá toàn bộ chỉ dẫn)");
        }

        Prescription prescription = findPrescription(req.getDonThuocId());
        validateStopInstructions(req.getInstructions());

        discontinuationRepository.deleteByPrescriptionId(prescription.getPrescriptionId());
        discontinuationRepository.flush();

        List<MedicationDiscontinuation> rows = discontinuationRepository.saveAll(req.getInstructions().stream()
                .map(i -> MedicationDiscontinuation.builder()
                        .prescriptionId(prescription.getPrescriptionId())
                        .medicationToStop(i.getThuocCanNgung())
                        .stopDate(i.getNgungTuNgay())
                        .discontinuationDays(i.getThoiGianNgungNgay())
                        .purpose(i.getMucDich())
                        .otherPurposeDescription(i.getMucDichKhacGhiRo())
                        .build())
                .toList());

        // ck_donthuoc_ngung_detail: the summary is required when the flag is true and must be null otherwise.
        boolean hasInstructions = !rows.isEmpty();
        prescription.setStopMedicationAtNextVisit(hasInstructions);
        prescription.setMedicationsToStopBeforeFollowUp(hasInstructions ? summarize(rows) : null);
        Prescription saved = prescriptionRepository.save(prescription);

        return StopMedicationResponse.from(saved, rows);
    }

    // ── PRESCRIPTIONS_GET_STOP_MEDICATION ──

    @Override
    @Transactional(readOnly = true)
    public StopMedicationResponse getStopMedication(Long prescriptionId) {
        if (prescriptionId == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: don_thuoc_id");
        }
        Prescription prescription = findPrescription(prescriptionId);
        checkPatientAccessToRecord(prescription.getMedicalRecordId());

        List<MedicationDiscontinuation> rows =
                discontinuationRepository.findByPrescriptionIdOrderByDiscontinuationIdAsc(prescriptionId);
        return StopMedicationResponse.from(prescription, rows);
    }

    // ── Helpers ──

    private Prescription findPrescription(Long prescriptionId) {
        return prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new AppException(ErrorCode.PRESCRIPTION_NOT_FOUND,
                        "Không tìm thấy đơn thuốc " + prescriptionId));
    }

    private static void requireId(Long value, String field) {
        if (value == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: " + field);
        }
    }

    private static void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "trang_thai_don không hợp lệ, chỉ chấp nhận: DANG_DIEU_TRI, HOAN_THANH, HUY");
        }
    }

    /** Patients may only read prescriptions of their own medical records. */
    private void checkPatientAccessToRecord(Long recordId) {
        if (!SecurityUtils.hasRole("BENH_NHAN")) {
            return;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        medicalRecordRepository.findById(recordId)
                .map(MedicalRecord::getPatientId)
                .filter(patientId -> !patientId.equals(currentUserId))
                .ifPresent(patientId -> {
                    throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
                });
    }

    private void validateItems(List<PrescriptionItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new AppException(ErrorCode.PRESCRIPTION_ITEMS_EMPTY);
        }

        Set<String> medicationIds = new LinkedHashSet<>();
        for (int i = 0; i < items.size(); i++) {
            PrescriptionItemRequest item = items.get(i);
            String label = "Thuốc thứ " + (i + 1);
            if (item == null) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, label + ": không được để trống");
            }

            boolean hasMedicationId = item.getMaThuoc() != null;
            boolean hasAdditionalName = item.getTenThuocBoSung() != null;
            if (hasMedicationId == hasAdditionalName) {
                throw new AppException(ErrorCode.PRESCRIPTION_ITEM_MEDICATION_CONFLICT,
                        label + ": phải có đúng một trong hai trường ma_thuoc hoặc ten_thuoc_bo_sung");
            }
            if (hasMedicationId) {
                checkOptionalText(item.getMaThuoc(), MEDICATION_ID_MAX, label + ": ma_thuoc");
                medicationIds.add(item.getMaThuoc());
            } else {
                checkOptionalText(item.getTenThuocBoSung(), ADDITIONAL_NAME_MAX, label + ": ten_thuoc_bo_sung");
            }
            checkRequiredText(item.getLieuDung(), DOSAGE_MAX, label + ": lieu_dung");
            checkRequiredText(item.getThoiGianDung(), DURATION_MAX, label + ": thoi_gian_dung");
            checkOptionalText(item.getMaKeHoachDieuTri(), TREATMENT_PLAN_ID_MAX, label + ": ma_ke_hoach_dieu_tri");
        }

        if (!medicationIds.isEmpty()) {
            Set<String> found = medicationRepository.findAllById(medicationIds).stream()
                    .map(Medication::getMedicationId)
                    .collect(Collectors.toSet());
            for (String id : medicationIds) {
                if (!found.contains(id)) {
                    throw new AppException(ErrorCode.MEDICATION_NOT_FOUND,
                            "Mã thuốc " + id + " không tồn tại trong danh mục thuốc");
                }
            }
        }
    }

    private List<PrescriptionItem> saveItems(Long prescriptionId, List<PrescriptionItemRequest> items) {
        List<PrescriptionItem> entities = new ArrayList<>(items.size());
        for (PrescriptionItemRequest item : items) {
            entities.add(PrescriptionItem.builder()
                    .prescriptionId(prescriptionId)
                    .medicationId(item.getMaThuoc())
                    .additionalMedicationName(item.getTenThuocBoSung())
                    .dosage(item.getLieuDung())
                    .usageDuration(item.getThoiGianDung())
                    .treatmentPlanId(item.getMaKeHoachDieuTri())
                    .build());
        }
        return prescriptionItemRepository.saveAll(entities);
    }

    private static void validateStopInstructions(List<StopInstructionRequest> instructions) {
        for (int i = 0; i < instructions.size(); i++) {
            StopInstructionRequest row = instructions.get(i);
            String label = "Chỉ dẫn thứ " + (i + 1);
            if (row == null) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, label + ": không được để trống");
            }

            checkRequiredText(row.getThuocCanNgung(), MEDICATION_TO_STOP_MAX, label + ": thuoc_can_ngung");
            if (row.getThoiGianNgungNgay() != null && row.getThoiGianNgungNgay() < 0) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                        label + ": thoi_gian_ngung_ngay phải lớn hơn hoặc bằng 0");
            }

            String purpose = row.getMucDich();
            if (purpose != null && !VALID_STOP_PURPOSES.contains(purpose)) {
                throw new AppException(ErrorCode.STOP_PURPOSE_INVALID,
                        label + ": muc_dich không hợp lệ, chỉ chấp nhận: Test huyet thanh tu than, Test kich thich, Khac");
            }
            // ck_ngung_mucdich_detail is biconditional: the description is required for Khac and forbidden otherwise.
            if (PURPOSE_OTHER.equals(purpose)) {
                if (row.getMucDichKhacGhiRo() == null || row.getMucDichKhacGhiRo().isBlank()) {
                    throw new AppException(ErrorCode.STOP_PURPOSE_OTHER_DESCRIPTION_REQUIRED,
                            label + ": muc_dich = Khac thì bắt buộc nhập muc_dich_khac_ghi_ro");
                }
                checkOptionalText(row.getMucDichKhacGhiRo(), OTHER_PURPOSE_MAX, label + ": muc_dich_khac_ghi_ro");
            } else if (row.getMucDichKhacGhiRo() != null) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                        label + ": chỉ nhập muc_dich_khac_ghi_ro khi muc_dich = Khac");
            }
        }
    }

    private static String summarize(List<MedicationDiscontinuation> rows) {
        return rows.stream()
                .map(MedicationDiscontinuation::getMedicationToStop)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private static void checkRequiredText(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, field + " là bắt buộc");
        }
        checkLength(value, maxLength, field);
    }

    /** Optional text may be absent, but when present it must not be blank (DB checks btrim(...) <> ''). */
    private static void checkOptionalText(String value, int maxLength, String field) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, field + " không được để trống");
        }
        checkLength(value, maxLength, field);
    }

    private static void checkLength(String value, int maxLength, String field) {
        if (value.length() > maxLength) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    field + " không được dài quá " + maxLength + " ký tự");
        }
    }
}
