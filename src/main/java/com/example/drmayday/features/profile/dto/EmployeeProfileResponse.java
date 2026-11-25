package com.example.drmayday.features.profile.dto;

import com.example.drmayday.entity.Employee;
import com.fasterxml.jackson.annotation.JsonProperty;

public record EmployeeProfileResponse(
        @JsonProperty("person_id") Long personId,
        @JsonProperty("branch_id") Long branchId) {

    public static EmployeeProfileResponse from(Employee employee) {
        return new EmployeeProfileResponse(employee.getEmployeeId(), employee.getBranchId());
    }
}
