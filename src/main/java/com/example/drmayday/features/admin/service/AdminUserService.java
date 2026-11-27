package com.example.drmayday.features.admin.service;

import com.example.drmayday.features.admin.dto.CreatePatientRequest;
import com.example.drmayday.features.admin.dto.CreatePersonRequest;
import com.example.drmayday.features.admin.dto.CreateStaffRequest;
import com.example.drmayday.features.admin.dto.CreateUserRequest;
import com.example.drmayday.features.admin.dto.CreatedUserResponse;
import com.example.drmayday.features.admin.dto.EditUserRequest;
import com.example.drmayday.features.admin.dto.UserDetailsResponse;
import com.example.drmayday.features.admin.dto.UserResponse;
import com.example.drmayday.features.profile.dto.EmployeeProfileResponse;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.PersonProfileResponse;
import org.springframework.data.domain.Page;

public interface AdminUserService {

    /**
     * ADMIN_GET_LIST_USERS - Retrieves the paginated list of users for an admin.
     *
     * @param page Current page number (1-based index)
     * @param limit Number of records per page
     * @return A page of users
     */
    Page<UserResponse> getListUsers(int page, int limit);

    /**
     * ADMIN_GET_USER_DETAILS - Retrieves a user's details by ID.
     *
     * @param id User ID as a string
     * @return User details
     */
    UserDetailsResponse getUserDetails(String id);

    /**
     * ADMIN_CREATE_USER - Creates a new user from the admin console.
     *
     * @param request Data for the new user
     * @return The created user's details
     */
    CreatedUserResponse createUser(CreateUserRequest request);

    PersonProfileResponse createPerson(CreatePersonRequest request);

    PatientProfileResponse createPatient(CreatePatientRequest request);

    EmployeeProfileResponse createStaff(CreateStaffRequest request);

    /**
     * ADMIN_EDIT_USER - Updates a user's full name, role, or active status.
     *
     * @param id ID of the user to update
     * @param request Updated user data
     * @return The updated user's details
     */
    UserResponse editUser(String id, EditUserRequest request);
}
