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
    OK("1000", "Thành công", HttpStatus.OK),
    CREATED("1001", "Tạo thành công", HttpStatus.CREATED),

    // 2xxx - Parameter errors (client errors)
    MISSING_REQUIRED_PARAMETER("2001", "Thiếu tham số bắt buộc", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER_TYPE("2002", "Kiểu tham số không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER_VALUE("2003", "Giá trị tham số không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER_FORMAT("2004", "Định dạng số điện thoại không hợp lệ", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("2005", "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("2006", "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),

    // 21xx - Facility business errors (branches and clinics)
    BRANCH_NOT_FOUND("2101", "Không tìm thấy chi nhánh", HttpStatus.NOT_FOUND),
    BRANCH_HAS_DEPENDENCIES("2102", "Chi nhánh vẫn còn phòng khám hoặc nhân viên", HttpStatus.BAD_REQUEST),
    ROOM_NOT_FOUND("2103", "Không tìm thấy phòng khám", HttpStatus.NOT_FOUND),
    ROOM_NAME_ALREADY_EXISTS("2104", "Tên phòng khám đã tồn tại trong chi nhánh này", HttpStatus.CONFLICT),
    ROOM_HAS_EXAM_SESSIONS("2105", "Phòng khám đang có phiên khám", HttpStatus.BAD_REQUEST),
    BRANCH_INACTIVE("2106", "Chi nhánh không hoạt động", HttpStatus.BAD_REQUEST),
    LAST_BRANCH_CANNOT_BE_DELETED("2107", "Không thể xóa chi nhánh cuối cùng", HttpStatus.BAD_REQUEST),

    // 3xxx - Authentication and authorization errors (Auth & RBAC)
    TOKEN_INVALID("3001", "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("3002", "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    USER_NOT_AUTHENTICATED("3003", "Người dùng chưa được xác thực", HttpStatus.UNAUTHORIZED),
    USER_PHONE_ALREADY_EXISTS("3006", "Số điện thoại người dùng đã tồn tại", HttpStatus.CONFLICT),
    USER_ACCOUNT_NOT_FOUND("3007", "Không tìm thấy tài khoản người dùng", HttpStatus.NOT_FOUND),
    PASSWORD_INCORRECT("3008", "Mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    ADMIN_ROLE_REQUIRED("3102", "Yêu cầu quyền quản trị viên", HttpStatus.FORBIDDEN),
    DOCTOR_ROLE_REQUIRED("3103", "Yêu cầu quyền bác sĩ", HttpStatus.FORBIDDEN),
    DOCTOR_NOT_ASSIGNED("3105", "Bác sĩ chưa được phân công vào hồ sơ", HttpStatus.BAD_REQUEST),
    CANNOT_ACCESS_ANOTHER_USER_DATA("3106", "Không thể truy cập dữ liệu của người dùng khác", HttpStatus.FORBIDDEN),

    // 4xxx - Medical record and form builder errors
    MEDICAL_RECORD_NOT_FOUND("4001", "Không tìm thấy hồ sơ bệnh án", HttpStatus.NOT_FOUND),
    ACTIVE_MEDICAL_RECORD_REQUIRED("4002", "Yêu cầu hồ sơ bệnh án đang hoạt động", HttpStatus.BAD_REQUEST),
    MEDICAL_RECORD_ALREADY_COMPLETED("4003", "Hồ sơ bệnh án đã hoàn tất", HttpStatus.BAD_REQUEST),
    TEMPLATE_NOT_FOUND("4101", "Không tìm thấy biểu mẫu", HttpStatus.NOT_FOUND),

    // 5xxx - UAS7 score errors
    UAS7_RECORD_ALREADY_EXISTS_TODAY("5002", "Đã tồn tại bản ghi điểm UAS7 trong ngày hôm nay", HttpStatus.CONFLICT),
    UAS7_SCORE_OUT_OF_RANGE("5003", "Điểm ISS7 hoặc HSS7 nằm ngoài khoảng cho phép (0-3)", HttpStatus.BAD_REQUEST),
    UAS7_NO_ACTIVE_RECORD("5004", "Bệnh nhân không có hồ sơ bệnh án đang hoạt động để ghi điểm UAS7", HttpStatus.BAD_REQUEST),

    // 9xxx - General system errors
    INTERNAL_SERVER_ERROR("9999", "Lỗi hệ thống nội bộ", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
