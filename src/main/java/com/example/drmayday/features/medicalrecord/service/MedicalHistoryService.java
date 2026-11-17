package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.features.medicalrecord.dto.AllergyContextResponse;
import com.example.drmayday.features.medicalrecord.dto.InfectionStatusResponse;
import com.example.drmayday.features.medicalrecord.dto.PreviousEpisodesResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAllergyContextRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertInfectionStatusRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertPreviousEpisodesRequest;

/**
 * Ghi bệnh sử theo hồ sơ (API 43 đến 45), theo spec 0003. Mỗi lần gọi ghi đè toàn bộ phần đó.
 */
public interface MedicalHistoryService {

    AllergyContextResponse upsertAllergyContext(UpsertAllergyContextRequest req);

    PreviousEpisodesResponse upsertPreviousEpisodes(UpsertPreviousEpisodesRequest req);

    InfectionStatusResponse upsertInfectionStatus(UpsertInfectionStatusRequest req);
}
