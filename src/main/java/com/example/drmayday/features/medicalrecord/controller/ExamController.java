package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.medicalrecord.dto.CreateExamRequest;
import com.example.drmayday.features.medicalrecord.dto.ExamDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.ExamResponse;
import com.example.drmayday.features.medicalrecord.dto.ExamSummaryResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateExamRequest;
import com.example.drmayday.features.medicalrecord.service.ExamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Controller phiên khám (Phân hệ Bệnh nhân & Hồ sơ bệnh án).
 * Chứa API 46 (create_exam), API 47 (list_exams), API 48 (get_exam_detail), API 49 (update_exam).
 */
@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /**
     * API 46: create_exam
     * Mở phiên khám trên một hồ sơ đang mở, kèm dòng nội dung khám rỗng đúng loại hồ sơ.
     * Phiên luôn đứng tên người gọi; lan_kham_thu không gửi thì lấy số lớn nhất của hồ sơ cộng 1.
     *
     * @param req record_id và thông tin chung của phiên
     * @return Phiên vừa tạo
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<ExamResponse>> createExam(@RequestBody CreateExamRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Khởi tạo phiên khám thành công", examService.createExam(req)));
    }

    /**
     * API 47: list_exams
     * Danh sách phiên của một hồ sơ, xếp theo lan_kham_thu tăng dần. Trả mảng, không phân trang.
     *
     * @param recordId Mã hồ sơ (bắt buộc, service tự trả 2001 khi thiếu)
     * @return Các phiên của hồ sơ
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','BENH_NHAN')")
    public ResponseEntity<BaseResponse<List<ExamSummaryResponse>>> listExams(
            @RequestParam(value = "record_id", required = false) String recordId) {
        return ResponseEntity.ok(BaseResponse.success(examService.listExams(recordId)));
    }

    /**
     * API 48: get_exam_detail
     * Chi tiết một phiên, kèm chi_tiet_lam_sang của phienkhamcap hoặc phienkhamman.
     *
     * @param id Mã phiên khám
     * @return Chi tiết phiên
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','BENH_NHAN')")
    public ResponseEntity<BaseResponse<ExamDetailResponse>> getExam(@PathVariable("id") String id) {
        return ResponseEntity.ok(BaseResponse.success(examService.getExam(id)));
    }

    /**
     * API 49: update_exam
     * Sửa thông tin chung của phiên, kiểu gộp: trường vắng mặt hoặc null giữ nguyên (khác API 43 đến 45).
     * Ghi chú gửi chuỗi rỗng thì xóa; sot = false không kèm nhiệt độ thì xóa nhiệt độ.
     *
     * @param id  Mã phiên khám
     * @param req Các trường cần sửa
     * @return Phiên sau khi sửa
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<ExamResponse>> updateExam(@PathVariable("id") String id,
                                                                 @RequestBody UpdateExamRequest req) {
        return ResponseEntity.ok(BaseResponse.success("Cập nhật phiên khám thành công", examService.updateExam(id, req)));
    }
}
