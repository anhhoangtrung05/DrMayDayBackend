package com.example.drmayday.features.testtype.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.testtype.dto.CreateTestTypeRequest;
import com.example.drmayday.features.testtype.dto.TestTypeResponse;
import com.example.drmayday.features.testtype.dto.UpdateTestTypeRequest;
import com.example.drmayday.features.testtype.service.TestTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/test-types")
@RequiredArgsConstructor
public class TestTypeController {

    private final TestTypeService testTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<TestTypeResponse>> createTestType(
            @RequestBody(required = false) CreateTestTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Created successfully", testTypeService.createTestType(request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<TestTypeResponse>>> getTestTypes() {
        return ResponseEntity.ok(BaseResponse.ok(testTypeService.getTestTypes()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<TestTypeResponse>> getTestType(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok(testTypeService.getTestType(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<TestTypeResponse>> updateTestType(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateTestTypeRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Updated successfully", testTypeService.updateTestType(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteTestType(@PathVariable Long id) {
        testTypeService.deleteTestType(id);
        return ResponseEntity.ok(BaseResponse.success("Deleted successfully", null));
    }
}
