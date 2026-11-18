package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.features.medicalrecord.dto.AddLesionDetailsBulkRequest;
import com.example.drmayday.features.medicalrecord.dto.LesionDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateLesionDetailRequest;

import java.util.List;

public interface LesionDetailsService {
    List<LesionDetailResponse> addLesionDetailsBulk(AddLesionDetailsBulkRequest req);
    List<LesionDetailResponse> getListLesionDetails(String examId);
    LesionDetailResponse updateLesionDetail(String id, String loaiBenhAn, UpdateLesionDetailRequest req);
    void deleteLesionDetail(String id, String loaiBenhAn);
}
