package com.example.drmayday.features.rbac.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {

    @JsonProperty("permission_name")
    @JsonAlias({"permissionName", "ten_quyen_han", "tenQuyenHan", "name"})
    @NotBlank(message = "2001")
    private String permissionName;

    @JsonProperty("description")
    @JsonAlias({"mo_ta", "moTa"})
    private String description;

    @JsonProperty("group_name")
    @JsonAlias({"groupName", "nhom_quyen", "nhomQuyen", "permissionGroup"})
    private String groupName;
}
