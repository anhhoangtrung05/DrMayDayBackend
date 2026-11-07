package com.example.drmayday.features.authorization.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditPermissionRequest {

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
