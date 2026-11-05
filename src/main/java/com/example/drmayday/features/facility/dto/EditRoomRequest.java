package com.example.drmayday.features.facility.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu sửa phòng khám (API 32 edit_room). Trường nào không gửi thì giữ nguyên.
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
