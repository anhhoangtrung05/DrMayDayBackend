package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.features.medicalrecord.dto.AddVitalSignsRequest;
import com.example.drmayday.features.medicalrecord.dto.VitalSignsResponse;

import java.util.List;

public interface VitalSignsService {
    VitalSignsResponse addVitalSigns(AddVitalSignsRequest req);
    List<VitalSignsResponse> getListVitalSigns(String examId);
}
