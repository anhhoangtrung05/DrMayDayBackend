package com.example.drmayday.features.authorization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO phản hồi chi tiết hồ sơ bệnh nhân (Dùng cho API 7 PROFILE_GET và API 8 PROFILE_PUT).
 */
public class PatientProfileResponse {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("phone")
    private String phone;

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

    public PatientProfileResponse() {
    }

    public PatientProfileResponse(Long userId, String fullName, String phone, String gender,
                                  Integer yearOfBirth, String address, String ethnic,
                                  String occupation, String bhytCode) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.gender = gender;
        this.yearOfBirth = yearOfBirth;
        this.address = address;
        this.ethnic = ethnic;
        this.occupation = occupation;
        this.bhytCode = bhytCode;
    }

    public PatientProfileResponse(Integer userId, String fullName, String phone, String gender,
                                  Integer yearOfBirth, String address, String ethnic,
                                  String occupation, String bhytCode) {
        this(userId != null ? userId.longValue() : null, fullName, phone, gender, yearOfBirth, address, ethnic, occupation, bhytCode);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId != null ? userId.longValue() : null;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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
        private Long userId;
        private String fullName;
        private String phone;
        private String gender;
        private Integer yearOfBirth;
        private String address;
        private String ethnic;
        private String occupation;
        private String bhytCode;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder userId(Integer userId) {
            this.userId = userId != null ? userId.longValue() : null;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
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

        public PatientProfileResponse build() {
            return new PatientProfileResponse(userId, fullName, phone, gender, yearOfBirth, address, ethnic, occupation, bhytCode);
        }
    }
}
