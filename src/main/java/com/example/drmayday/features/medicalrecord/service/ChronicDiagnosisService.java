package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.features.medicalrecord.dto.ChronicDiagnosisResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertChronicDiagnosisRequest;

public interface ChronicDiagnosisService {

    ChronicDiagnosisResponse upsertChronicDiagnosis(UpsertChronicDiagnosisRequest req);
}
