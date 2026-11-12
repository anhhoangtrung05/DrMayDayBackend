package com.example.drmayday.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Standardized response codes and HTTP status codes used by the DrMayDay system.
 * Refer to the "Response Codes" sheet.
 */
@Getter
public enum ErrorCode {

    // 1xxx - Success
    OK("1000", "OK", HttpStatus.OK),
    CREATED("1001", "Created successfully", HttpStatus.CREATED),

    // 2xxx - Parameter errors (client errors)
    MISSING_REQUIRED_PARAMETER("2001", "Missing required parameter", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER_TYPE("2002", "Invalid parameter type", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER_VALUE("2003", "Invalid parameter value", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER_FORMAT("2004", "Invalid phone number format", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("2005", "Bad request", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("2006", "Resource not found", HttpStatus.NOT_FOUND),

    // 21xx - Facility business errors (branches and clinics)
    BRANCH_NOT_FOUND("2101", "Branch not found", HttpStatus.NOT_FOUND),
    BRANCH_HAS_DEPENDENCIES("2102", "Branch still has rooms or staff", HttpStatus.BAD_REQUEST),
    ROOM_NOT_FOUND("2103", "Room not found", HttpStatus.NOT_FOUND),
    ROOM_NAME_ALREADY_EXISTS("2104", "Room name already exists in this branch", HttpStatus.CONFLICT),
    ROOM_HAS_EXAM_SESSIONS("2105", "Room has exam sessions", HttpStatus.BAD_REQUEST),
    BRANCH_INACTIVE("2106", "Branch is inactive", HttpStatus.BAD_REQUEST),
    LAST_BRANCH_CANNOT_BE_DELETED("2107", "Last branch cannot be deleted", HttpStatus.BAD_REQUEST),

    // 3xxx - Authentication and authorization errors (Auth & RBAC)
    TOKEN_INVALID("3001", "Token invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("3002", "Token expired", HttpStatus.UNAUTHORIZED),
    USER_NOT_AUTHENTICATED("3003", "User not authenticated", HttpStatus.UNAUTHORIZED),
    USER_PHONE_ALREADY_EXISTS("3006", "User phone already exists", HttpStatus.CONFLICT),
    USER_ACCOUNT_NOT_FOUND("3007", "User account not found", HttpStatus.NOT_FOUND),
    PASSWORD_INCORRECT("3008", "Password incorrect", HttpStatus.UNAUTHORIZED),
    ADMIN_ROLE_REQUIRED("3102", "Admin role required", HttpStatus.FORBIDDEN),
    DOCTOR_ROLE_REQUIRED("3103", "Doctor role required", HttpStatus.FORBIDDEN),
    DOCTOR_NOT_ASSIGNED("3105", "Doctor not assigned to record", HttpStatus.BAD_REQUEST),
    CANNOT_ACCESS_ANOTHER_USER_DATA("3106", "Cannot access another user data", HttpStatus.FORBIDDEN),

    // 4xxx - Medical record and form builder errors
    MEDICAL_RECORD_NOT_FOUND("4001", "Medical record not found", HttpStatus.NOT_FOUND),
    ACTIVE_MEDICAL_RECORD_REQUIRED("4002", "Active medical record required", HttpStatus.BAD_REQUEST),
    MEDICAL_RECORD_ALREADY_COMPLETED("4003", "Medical record already completed", HttpStatus.BAD_REQUEST),
    TEMPLATE_NOT_FOUND("4101", "Template not found", HttpStatus.NOT_FOUND),

    // 5xxx - UAS7 score errors
    UAS7_RECORD_ALREADY_EXISTS_TODAY("5002", "UAS7 score record already exists for today", HttpStatus.CONFLICT),
    UAS7_SCORE_OUT_OF_RANGE("5003", "ISS7 or HSS7 score out of range (0-3)", HttpStatus.BAD_REQUEST),
    UAS7_NO_ACTIVE_RECORD("5004", "Patient has no active medical record for UAS7", HttpStatus.BAD_REQUEST),

    // 9xxx - General system errors
    INTERNAL_SERVER_ERROR("9999", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
