package com.example.drmayday.features.profile.dto;

import com.example.drmayday.entity.Person;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PersonProfileResponse(
        @JsonProperty("person_id") Long personId,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("date_of_birth") LocalDate dateOfBirth,
        @JsonProperty("gender") String gender,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("province_city") String provinceCity,
        @JsonProperty("ward_commune") String wardCommune,
        @JsonProperty("address_line") String addressLine,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {

    public static PersonProfileResponse from(Person person) {
        return new PersonProfileResponse(
                person.getPersonId(),
                person.getFullName(),
                person.getBirthDate(),
                person.getGender(),
                person.getPhoneNumber(),
                person.getProvinceCity(),
                person.getCommuneWard(),
                person.getStreetAddress(),
                person.getCreatedAt(),
                person.getUpdatedAt());
    }
}
