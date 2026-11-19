package com.example.drmayday.features.uas7.service;

import com.example.drmayday.features.uas7.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface Uas7Service {
    Uas7EligibilityResponse checkEligibility();
    Uas7ScoreResponse createScore(CreateUas7ScoreRequest request);
    List<Uas7ScoreResponse> getPatientScores(String patientId, LocalDate from, LocalDate to, String cycleId);
    Uas7CycleResponse createCycle(String patientId, CreateUas7CycleRequest request);
    List<Uas7CycleResponse> getPatientCycles(String patientId);
    Uas7CycleDetailResponse getCycleDetail(String cycleId);
    Uas7CycleResponse updateCycleStatus(String cycleId, UpdateUas7CycleStatusRequest request);
}
