package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.features.medicalrecord.dto.AcuteExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.ChronicExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAcuteExamFindingsRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertChronicExamFindingsRequest;

public interface ExamFindingsService {
    AcuteExamFindingsResponse upsertAcuteExamFindings(UpsertAcuteExamFindingsRequest req);
    ChronicExamFindingsResponse upsertChronicExamFindings(UpsertChronicExamFindingsRequest req);
}
