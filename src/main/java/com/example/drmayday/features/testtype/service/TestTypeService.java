package com.example.drmayday.features.testtype.service;

import com.example.drmayday.features.testtype.dto.CreateTestTypeRequest;
import com.example.drmayday.features.testtype.dto.TestTypeResponse;
import com.example.drmayday.features.testtype.dto.UpdateTestTypeRequest;

import java.util.List;

public interface TestTypeService {

    TestTypeResponse createTestType(CreateTestTypeRequest request);

    List<TestTypeResponse> getTestTypes();

    TestTypeResponse getTestType(Long id);

    TestTypeResponse updateTestType(Long id, UpdateTestTypeRequest request);

    void deleteTestType(Long id);
}
