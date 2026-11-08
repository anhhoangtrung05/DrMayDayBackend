package com.example.drmayday.features.facility.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu tạo chi nhánh mới (API 24 set_branch).
 */
public class CreateBranchRequest {

    @NotBlank(message = "2001")
    @Size(max = 150, message = "2003")
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "2001")
    @Size(max = 500, message = "2003")
    @JsonProperty("address")
    private String address;

    // Cho phép chuỗi rỗng và khoảng trắng đầu cuối, service sẽ trim rồi đổi rỗng thành NULL
    @Pattern(regexp = "^\\s*(\\+?[0-9]{8,15})?\\s*$", message = "2004")
    @JsonProperty("phone")
    private String phone;

    public CreateBranchRequest() {
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
}
