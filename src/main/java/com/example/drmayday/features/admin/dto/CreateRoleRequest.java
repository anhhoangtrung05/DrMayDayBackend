package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleRequest {
    @NotBlank(message = "2001")
    @JsonProperty("role_name")
    @JsonAlias({"roleName", "ten_vai_tro", "tenVaiTro"})
    private String roleName;

    @JsonProperty("description")
    @JsonAlias({"mo_ta", "moTa"})
    private String description;

    @JsonProperty("parent_role_id")
    @JsonAlias({"parentRoleId", "ma_vai_tro_cha", "maVaiTroCha"})
    private String parentRoleId;
}
