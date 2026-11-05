package com.example.drmayday.features.facility.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu tạo phòng khám mới (API 29 set_room).
 * branch_id là chuỗi và được parse trong service để trả đúng mã 2002.
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
