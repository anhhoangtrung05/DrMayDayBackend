package com.drmayday.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Bảng mã phản hồi chuẩn hóa (System Response Codes & Status Codes) hệ thống DrMayDay.
 * Tham chiếu trực tiếp từ Sheet "MÃ PHẢN HỒI".
 */
@Getter
public enum ErrorCode {

    // 1xxx - Thành công
    OK("1000", "OK", HttpStatus.OK),
    CREATED("1001", "Created successfully", HttpStatus.CREATED),

    // 2xxx - Lỗi tham số (Client error)
    MISSING_REQUIRED_PARAMETER("2001", "Missing required parameter", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER_TYPE("2002", "Invalid parameter type", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER_VALUE("2003", "Invalid parameter value", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER_FORMAT("2004", "Invalid phone number format", HttpStatus.BAD_REQUEST),

    // 3xxx - Lỗi xác thực & phân quyền (Auth & RBAC)
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

    // 4xxx - Lỗi Hồ sơ bệnh án & Form Builder
    MEDICAL_RECORD_NOT_FOUND("4001", "Medical record not found", HttpStatus.NOT_FOUND),
    ACTIVE_MEDICAL_RECORD_REQUIRED("4002", "Active medical record required", HttpStatus.BAD_REQUEST),
    MEDICAL_RECORD_ALREADY_COMPLETED("4003", "Medical record already completed", HttpStatus.BAD_REQUEST),
    TEMPLATE_NOT_FOUND("4101", "Template not found", HttpStatus.NOT_FOUND),

    // 5xxx - Lỗi Thang điểm UAS7
    UAS7_RECORD_ALREADY_EXISTS_TODAY("5002", "UAS7 score record already exists for today", HttpStatus.CONFLICT),
    UAS7_SCORE_OUT_OF_RANGE("5003", "ISS7 or HSS7 score out of range (0-3)", HttpStatus.BAD_REQUEST),
    UAS7_NO_ACTIVE_RECORD("5004", "Patient has no active medical record for UAS7", HttpStatus.BAD_REQUEST),

    // 9xxx - Lỗi hệ thống chung
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
