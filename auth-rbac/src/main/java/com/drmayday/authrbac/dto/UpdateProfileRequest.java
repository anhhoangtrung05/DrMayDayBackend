package com.drmayday.authrbac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO yêu cầu cập nhật hồ sơ bệnh nhân (Dùng cho API 8 PROFILE_PUT).
 */
public class UpdateProfileRequest {

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("year_of_birth")
    private Integer yearOfBirth;

    @JsonProperty("address")
    private String address;

    @JsonProperty("ethnic")
    private String ethnic;

    @JsonProperty("occupation")
    private String occupation;

    @JsonProperty("bhyt_code")
    private String bhytCode;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String fullName, String gender, Integer yearOfBirth,
                                String address, String ethnic, String occupation, String bhytCode) {
        this.fullName = fullName;
        this.gender = gender;
        this.yearOfBirth = yearOfBirth;
        this.address = address;
        this.ethnic = ethnic;
        this.occupation = occupation;
        this.bhytCode = bhytCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(Integer yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEthnic() {
        return ethnic;
    }

    public void setEthnic(String ethnic) {
        this.ethnic = ethnic;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getBhytCode() {
        return bhytCode;
    }

    public void setBhytCode(String bhytCode) {
        this.bhytCode = bhytCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String fullName;
        private String gender;
        private Integer yearOfBirth;
        private String address;
        private String ethnic;
        private String occupation;
        private String bhytCode;

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder yearOfBirth(Integer yearOfBirth) {
            this.yearOfBirth = yearOfBirth;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder ethnic(String ethnic) {
            this.ethnic = ethnic;
            return this;
        }

        public Builder occupation(String occupation) {
            this.occupation = occupation;
            return this;
        }

        public Builder bhytCode(String bhytCode) {
            this.bhytCode = bhytCode;
            return this;
        }

        public UpdateProfileRequest build() {
            return new UpdateProfileRequest(fullName, gender, yearOfBirth, address, ethnic, occupation, bhytCode);
        }
    }
}
