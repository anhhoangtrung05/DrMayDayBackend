package com.example.drmayday.features.lab.service;

import com.example.drmayday.features.lab.dto.LabResultResponse;
import com.example.drmayday.features.lab.dto.UpsertLabResultsRequest;

public interface LabResultService {

    LabResultResponse upsertLabResults(UpsertLabResultsRequest req);

    LabResultResponse getLabResults(Long examId);
}
