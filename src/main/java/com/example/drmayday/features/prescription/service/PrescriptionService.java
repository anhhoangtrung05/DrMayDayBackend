package com.example.drmayday.features.prescription.service;

import com.example.drmayday.features.prescription.dto.CreatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.PrescriptionResponse;
import com.example.drmayday.features.prescription.dto.StopMedicationResponse;
import com.example.drmayday.features.prescription.dto.UpdatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.UpsertStopMedicationRequest;

import java.util.List;

public interface PrescriptionService {

    PrescriptionResponse createPrescription(CreatePrescriptionRequest req);

    List<PrescriptionResponse> getPrescriptions(Long examId, Long recordId);

    PrescriptionResponse updatePrescription(Long prescriptionId, UpdatePrescriptionRequest req);

    StopMedicationResponse upsertStopMedication(UpsertStopMedicationRequest req);

    StopMedicationResponse getStopMedication(Long prescriptionId);
}
