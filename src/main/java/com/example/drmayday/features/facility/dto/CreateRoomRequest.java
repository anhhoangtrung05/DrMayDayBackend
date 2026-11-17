package com.example.drmayday.features.facility.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for TOPOLOGY_SET_ROOM.
 * branch_id is a string and is parsed in the service to return the correct error code (2002).
 */
public class CreateRoomRequest {

    @NotBlank(message = "2001")
    @JsonProperty("branch_id")
    private String branchId;

    @NotBlank(message = "2001")
    @Size(max = 150, message = "2003")
    @JsonProperty("name")
    private String name;

    public CreateRoomRequest() {
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
