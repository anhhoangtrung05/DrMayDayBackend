package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignUserRequest {
    @NotBlank(message = "2001")
    @JsonProperty("user_id") @JsonAlias("userId")
    private String userId;
    @NotBlank(message = "2001")
    @JsonProperty("branch_id") @JsonAlias("branchId")
    private String branchId;
    @NotBlank(message = "2001")
    @JsonProperty("department_id") @JsonAlias("departmentId")
    private String departmentId;
    @NotBlank(message = "2001")
    @JsonProperty("room_id") @JsonAlias("roomId")
    private String roomId;
}
