package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StaffAssignResponse {

    @JsonProperty("staff_id")
    private String staffId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("branch_id")
    private String branchId;

    @JsonProperty("branch_name")
    private String branchName;
}
