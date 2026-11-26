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
public class CreateRoleRequest {

    @JsonProperty("role_name")
    @JsonAlias({"roleName", "ten_vai_tro", "tenVaiTro", "name"})
    @NotBlank(message = "2001")
    private String roleName;

    @JsonProperty("display_name")
    @JsonAlias({"displayName", "ten_hien_thi", "tenHienThi"})
    private String displayName;

    @JsonProperty("description")
    @JsonAlias({"mo_ta_vai_tro", "moTaVaiTro", "mo_ta"})
    private String description;
}
