package com.drmayday.authrbac.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetRolePermissionsRequest {

    @JsonProperty("permission_ids")
    private List<String> permissionIds;
}
