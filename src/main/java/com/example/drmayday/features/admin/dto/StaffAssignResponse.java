package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffAssignResponse {
    @JsonProperty("staff_id") private String staffId;
    @JsonProperty("user_id") private String userId;
    @JsonProperty("branch_id") private String branchId;
    @JsonProperty("department_id") private String departmentId;
    @JsonProperty("room_id") private String roomId;
}
