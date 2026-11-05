package com.example.drmayday.features.facility.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu sửa chi nhánh (API 27 edit_branch). Trường nào không gửi thì giữ nguyên.
 */
public class EditBranchRequest {

    @Size(max = 150, message = "2003")
    @JsonProperty("name")
    private String name;

    @Size(max = 500, message = "2003")
    @JsonProperty("address")
    private String address;

    // phone rỗng nghĩa là xóa số điện thoại
    @Pattern(regexp = "^\\s*(\\+?[0-9]{8,15})?\\s*$", message = "2004")
    @JsonProperty("phone")
    private String phone;

    @JsonProperty("status")
    private Boolean status;

    public EditBranchRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
