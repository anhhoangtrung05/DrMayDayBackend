package com.example.drmayday.features.authorization.dto;

import com.example.drmayday.entity.Permission;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PermissionResponse {

    @JsonProperty("permission_id")
    @JsonAlias({"permissionId", "ma_quyen_han", "maQuyenHan"})
    private String permissionId;

    @JsonProperty("permission_name")
    @JsonAlias({"permissionName", "ten_quyen_han", "tenQuyenHan", "name"})
    private String permissionName;

    @JsonProperty("description")
    @JsonAlias({"mo_ta", "moTa"})
    private String description;

    @JsonProperty("group_name")
    @JsonAlias({"groupName", "nhom_quyen", "nhomQuyen", "permissionGroup"})
    private String groupName;

    public static PermissionResponse fromEntity(Permission permission) {
        if (permission == null) {
            return null;
        }
        return PermissionResponse.builder()
                .permissionId(String.valueOf(permission.getPermissionId()))
                .permissionName(permission.getName())
                .description(permission.getDescription())
                .groupName(permission.getPermissionGroup())
                .build();
    }
}
