package com.example.drmayday.features.admin.dto;

import com.example.drmayday.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleResponse {
    @JsonProperty("role_id") private String roleId;
    @JsonProperty("role_name") private String roleName;
    @JsonProperty("display_name") private String displayName;
    @JsonProperty("description") private String description;
    @JsonProperty("parent_role_id") private String parentRoleId;

    public static RoleResponse fromEntity(Role role) {
        return RoleResponse.builder()
                .roleId(String.valueOf(role.getRoleId()))
                .roleName(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .parentRoleId(role.getParentRoleId() == null ? null : String.valueOf(role.getParentRoleId()))
                .build();
    }
}
