package com.example.drmayday.features.uas7.service;

import com.example.drmayday.features.uas7.dto.CreateUas7CycleRequest;
import com.example.drmayday.features.uas7.dto.CreateUas7ScoreRequest;
import com.example.drmayday.features.uas7.dto.Uas7CycleDetailResponse;
import com.example.drmayday.features.uas7.dto.Uas7CycleResponse;
import com.example.drmayday.features.uas7.dto.Uas7EligibilityResponse;
import com.example.drmayday.features.uas7.dto.Uas7ScoreResponse;
import com.example.drmayday.features.uas7.dto.UpdateUas7CycleStatusRequest;

import java.time.LocalDate;
import java.util.List;

public interface Uas7Service {

    Uas7EligibilityResponse checkEligibility();

    Uas7ScoreResponse createScore(CreateUas7ScoreRequest request);

    List<Uas7ScoreResponse> getPatientScores(String patientId, LocalDate fromDate, LocalDate toDate, String cycleId);

    Uas7CycleResponse createCycle(String patientId, CreateUas7CycleRequest request);

    List<Uas7CycleResponse> getPatientCycles(String patientId);

    Uas7CycleDetailResponse getCycleDetail(String cycleId);

    Uas7CycleResponse updateCycleStatus(String cycleId, UpdateUas7CycleStatusRequest request);
}
