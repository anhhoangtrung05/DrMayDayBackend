package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignUserRequest {

    @JsonProperty("user_id")
    @JsonAlias({"userId", "ma_tai_khoan"})
    @NotNull(message = "2001")
    private Long userId;

    @JsonProperty("branch_id")
    @JsonAlias({"branchId", "ma_chi_nhanh"})
    @NotNull(message = "2001")
    private Long branchId;
}
