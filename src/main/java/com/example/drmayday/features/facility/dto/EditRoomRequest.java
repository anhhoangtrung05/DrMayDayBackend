package com.example.drmayday.features.facility.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for TOPOLOGY_EDIT_ROOM. Omitted fields remain unchanged.
 */
public class EditRoomRequest {

    @JsonProperty("branch_id")
    private String branchId;

    @Size(max = 150, message = "2003")
    @JsonProperty("name")
    private String name;

    public EditRoomRequest() {
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
