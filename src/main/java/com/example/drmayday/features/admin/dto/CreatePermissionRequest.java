package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePermissionRequest {
    @NotBlank(message = "2001")
    @JsonProperty("permission_name")
    @JsonAlias({"permissionName", "ten_quyen_han", "tenQuyenHan"})
    private String permissionName;

    @JsonProperty("description")
    @JsonAlias({"mo_ta", "moTa"})
    private String description;

    @JsonProperty("group_name")
    @JsonAlias({"groupName", "nhom_quyen", "nhomQuyen"})
    private String groupName;
}
