package com.drmayday.authrbac.dto.request;

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
    private String permissionName;

    private String description;
}
