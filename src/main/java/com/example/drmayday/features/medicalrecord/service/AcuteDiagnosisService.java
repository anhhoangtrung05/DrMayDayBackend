package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.features.medicalrecord.dto.AcuteDiagnosisResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAcuteDiagnosisRequest;

public interface AcuteDiagnosisService {

    AcuteDiagnosisResponse upsertAcuteDiagnosis(UpsertAcuteDiagnosisRequest req);
}
