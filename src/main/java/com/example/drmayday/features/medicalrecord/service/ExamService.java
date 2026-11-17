package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.features.medicalrecord.dto.CreateExamRequest;
import com.example.drmayday.features.medicalrecord.dto.ExamDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.ExamResponse;
import com.example.drmayday.features.medicalrecord.dto.ExamSummaryResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateExamRequest;

import java.util.List;

/**
 * Nghiệp vụ phiên khám (API 46 đến 49), theo spec 0004.
 */
public interface ExamService {

    ExamResponse createExam(CreateExamRequest req);

    List<ExamSummaryResponse> listExams(String recordId);

    ExamDetailResponse getExam(String id);

    ExamResponse updateExam(String id, UpdateExamRequest req);
}
