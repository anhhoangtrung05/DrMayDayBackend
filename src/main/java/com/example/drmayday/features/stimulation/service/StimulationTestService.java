package com.example.drmayday.features.stimulation.service;

import com.example.drmayday.features.stimulation.dto.StimulationTestResultInput;
import com.example.drmayday.features.stimulation.dto.StimulationTestResultResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionDetailResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionResponse;
import com.example.drmayday.features.stimulation.dto.UpsertStimulationTestSessionRequest;

import java.util.List;

public interface StimulationTestService {

    StimulationTestSessionResponse upsertSession(Long hoSoId, Long phienKhamId, UpsertStimulationTestSessionRequest request);

    StimulationTestSessionDetailResponse getSessionDetail(Long hoSoId, Long phienKhamId);

    List<StimulationTestResultResponse> createResultsBulk(Long hoSoId, Long phienKhamId, List<StimulationTestResultInput> requests);

    StimulationTestResultResponse updateResult(Long id, StimulationTestResultInput request);

    void deleteResult(Long id);
}
