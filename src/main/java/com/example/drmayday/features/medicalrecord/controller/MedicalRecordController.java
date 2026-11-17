package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.common.dto.PageResponse;
import com.example.drmayday.features.medicalrecord.dto.CreateMedicalRecordRequest;
import com.example.drmayday.features.medicalrecord.dto.ExamHistoryResponse;
import com.example.drmayday.features.medicalrecord.dto.MedicalRecordListItem;
import com.example.drmayday.features.medicalrecord.dto.MedicalRecordResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateMedicalRecordRequest;
import com.example.drmayday.features.medicalrecord.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller hồ sơ bệnh án lõi (Phân hệ Bệnh nhân & Hồ sơ bệnh án).
 * Chứa API 34 (create_medical_record), API 35 (list_medical_records), API 36 (get_medical_record),
 * API 37 (update_medical_record), API 38 (delete_medical_record), API 39 (get_patient_record_history).
 * Ma trận vai trò nằm đủ trong SecurityConfig; @PreAuthorize ở đây là lớp thứ hai.
 */
@RestController
@RequestMapping("/api/v1")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    /**
     * API 34: create_medical_record
     * Bác sĩ hoặc Admin tạo hồ sơ CAP hoặc MAN cho bệnh nhân, kèm subtype rỗng cùng loại.
     *
     * @param patientId Mã bệnh nhân (= ma_benh_nhan)
     * @param req Dữ liệu hồ sơ mới
     * @return Hồ sơ vừa tạo
     */
    @PostMapping("/patients/{patient_id}/medical-records")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<MedicalRecordResponse>> createMedicalRecord(
            @PathVariable("patient_id") String patientId,
            @Valid @RequestBody CreateMedicalRecordRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(medicalRecordService.createMedicalRecord(patientId, req)));
    }

    /**
     * API 35: list_medical_records
     * Danh sách hồ sơ có phân trang, lọc theo bệnh nhân, loại và trạng thái.
     *
     * @param patientId Lọc theo mã bệnh nhân
     * @param medicalRecordType Lọc theo loại (CAP hoặc MAN)
     * @param status Lọc theo trạng thái
     * @param page Trang, bắt đầu từ 1
     * @param size Số dòng mỗi trang, từ 1 đến 100
     * @return Trang hồ sơ
     */
    @GetMapping("/medical-records")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<PageResponse<MedicalRecordListItem>>> listMedicalRecords(
            @RequestParam(name = "patient_id", required = false) String patientId,
            @RequestParam(name = "loai_benh_an", required = false) String medicalRecordType,
            @RequestParam(name = "trang_thai", required = false) String status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        return ResponseEntity.ok(BaseResponse.success(
                medicalRecordService.listMedicalRecords(patientId, medicalRecordType, status, page, size)));
    }

    /**
     * API 36: get_medical_record
     * Chi tiết hồ sơ cùng chẩn đoán và các bảng 1:1 theo loại. Bệnh nhân chỉ xem được hồ sơ của mình.
     *
     * @param id Mã hồ sơ (= ma_ho_so)
     * @return Chi tiết hồ sơ
     */
    @GetMapping("/medical-records/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','BENH_NHAN')")
    public ResponseEntity<BaseResponse<MedicalRecordResponse>> getMedicalRecord(@PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success(medicalRecordService.getMedicalRecord(id)));
    }

    /**
     * API 37: update_medical_record
     * Sửa trường hành chính và trạng thái. Trường vắng mặt hoặc null giữ nguyên.
     *
     * @param id Mã hồ sơ
     * @param req Các trường cần sửa
     * @return Hồ sơ sau khi sửa
     */
    @PutMapping("/medical-records/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<MedicalRecordResponse>> updateMedicalRecord(
            @PathVariable String id,
            @Valid @RequestBody UpdateMedicalRecordRequest req) {
        return ResponseEntity.ok(BaseResponse.success(medicalRecordService.updateMedicalRecord(id, req)));
    }

    /**
     * API 38: delete_medical_record
     * Xóa hồ sơ không còn dữ liệu con.
     *
     * @param id Mã hồ sơ
     * @return Phản hồi thành công, không có data
     */
    @DeleteMapping("/medical-records/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<Void>> deleteMedicalRecord(@PathVariable String id) {
        medicalRecordService.deleteMedicalRecord(id);
        return ResponseEntity.ok(BaseResponse.success("Xóa hồ sơ thành công", null));
    }

    /**
     * API 39: get_patient_record_history
     * Lịch sử phiên khám của một bệnh nhân, mới nhất trước. Bệnh nhân chỉ xem được của mình.
     *
     * @param patientId Mã bệnh nhân
     * @return Danh sách phiên khám
     */
    @GetMapping("/patients/{patient_id}/medical-records/history")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','BENH_NHAN')")
    public ResponseEntity<BaseResponse<List<ExamHistoryResponse>>> getPatientRecordHistory(
            @PathVariable("patient_id") String patientId) {
        return ResponseEntity.ok(BaseResponse.success(medicalRecordService.getPatientRecordHistory(patientId)));
    }
}
