package com.example.drmayday.features.authorization.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetRolePermissionsRequest {

    @JsonProperty("permission_ids")
    @JsonAlias({"permissionIds", "ma_quyen_han", "maQuyenHan"})
    private List<String> permissionIds;
}
