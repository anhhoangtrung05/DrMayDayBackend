package com.drmayday.authrbac.dto.response;

import com.drmayday.authrbac.entity.Permission;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    @JsonProperty("permission_id")
    private String permissionId;

    @JsonProperty("permission_name")
    private String permissionName;

    private String description;

    public static PermissionResponse fromEntity(Permission permission) {
        if (permission == null) {
            return null;
        }
        return PermissionResponse.builder()
                .permissionId(String.valueOf(permission.getPermissionId()))
                .permissionName(permission.getName())
                .description(permission.getDescription())
                .build();
    }
}
