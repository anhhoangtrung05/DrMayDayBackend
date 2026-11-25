package com.example.drmayday.features.profile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class UpdateEmployeeProfileRequest {

    @JsonProperty("person_id")
    @NotNull(message = "2001")
    private Long personId;

    @JsonProperty("branch_id")
    @NotNull(message = "2001")
    private Long branchId;

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }
}
