package com.example.drmayday.features.rbac.dto;

import com.example.drmayday.entity.Role;
import com.fasterxml.jackson.annotation.JsonAlias;
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
public class RoleResponse {

    @JsonProperty("role_id")
    @JsonAlias({"roleId", "ma_vai_tro"})
    private String roleId;

    @JsonProperty("role_name")
    @JsonAlias({"roleName", "ten_vai_tro"})
    private String roleName;

    @JsonProperty("display_name")
    @JsonAlias({"displayName", "ten_hien_thi"})
    private String displayName;

    @JsonProperty("description")
    @JsonAlias({"mo_ta_vai_tro", "mo_ta"})
    private String description;

    public static RoleResponse fromEntity(Role role) {
        if (role == null) {
            return null;
        }
        return RoleResponse.builder()
                .roleId(String.valueOf(role.getRoleId()))
                .roleName(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .build();
    }
}
