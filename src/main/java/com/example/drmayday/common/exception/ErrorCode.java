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
    PATIENT_PROFILE_NOT_FOUND("3009", "Không tìm thấy hồ sơ bệnh nhân", HttpStatus.NOT_FOUND),
    EMPLOYEE_PROFILE_NOT_FOUND("3010", "Không tìm thấy hồ sơ nhân viên", HttpStatus.NOT_FOUND),
    PERSON_ALREADY_HAS_USER_ACCOUNT("3011", "Người đã có tài khoản", HttpStatus.CONFLICT),
    PERSON_ALREADY_REGISTERED_AS_PATIENT("3012", "Người đã có hồ sơ bệnh nhân", HttpStatus.CONFLICT),
    PERSON_ALREADY_REGISTERED_AS_EMPLOYEE("3013", "Người đã có hồ sơ nhân viên", HttpStatus.CONFLICT),
    PERSON_NOT_FOUND("3014", "Không tìm thấy người", HttpStatus.NOT_FOUND),
    PATIENT_HIS_CODE_ALREADY_EXISTS("3015", "Mã HIS bệnh nhân đã tồn tại", HttpStatus.CONFLICT),
    ADMIN_ROLE_REQUIRED("3102", "Yêu cầu quyền quản trị viên", HttpStatus.FORBIDDEN),
    DOCTOR_ROLE_REQUIRED("3103", "Yêu cầu quyền bác sĩ", HttpStatus.FORBIDDEN),
    DOCTOR_NOT_ASSIGNED("3105", "Bác sĩ chưa được phân công vào hồ sơ", HttpStatus.BAD_REQUEST),
    CANNOT_ACCESS_ANOTHER_USER_DATA("3106", "Không thể truy cập dữ liệu của người dùng khác", HttpStatus.FORBIDDEN),
    ROLE_ALREADY_EXISTS("3107", "Tên vai trò đã tồn tại", HttpStatus.CONFLICT),
    PERMISSION_ALREADY_EXISTS("3108", "Tên quyền hạn đã tồn tại", HttpStatus.CONFLICT),

    // 4xxx - Medical record and form builder errors
    MEDICAL_RECORD_NOT_FOUND("4001", "Không tìm thấy hồ sơ bệnh án", HttpStatus.NOT_FOUND),
    ACTIVE_MEDICAL_RECORD_REQUIRED("4002", "Yêu cầu hồ sơ bệnh án đang hoạt động", HttpStatus.BAD_REQUEST),
    MEDICAL_RECORD_ALREADY_COMPLETED("4003", "Hồ sơ bệnh án đã hoàn tất", HttpStatus.BAD_REQUEST),
    PATIENT_ALREADY_HAS_ACTIVE_RECORD("4006", "Bệnh nhân đã có hồ sơ bệnh án đang hoạt động", HttpStatus.CONFLICT),
    MEDICAL_RECORD_HAS_CHILD_DATA("4007", "Hồ sơ bệnh án vẫn còn dữ liệu con", HttpStatus.CONFLICT),
    RECORD_TYPE_MISMATCH("4008", "Loại bệnh án không khớp", HttpStatus.BAD_REQUEST),
    TEMPLATE_NOT_FOUND("4101", "Không tìm thấy biểu mẫu", HttpStatus.NOT_FOUND),

    // 5xxx - UAS7 score errors
    UAS7_RECORD_ALREADY_EXISTS_TODAY("5002", "Đã tồn tại bản ghi điểm UAS7 trong ngày hôm nay", HttpStatus.CONFLICT),
    UAS7_SCORE_OUT_OF_RANGE("5003", "Điểm ISS7 hoặc HSS7 nằm ngoài khoảng cho phép (0-3)", HttpStatus.BAD_REQUEST),
    UAS7_NO_ACTIVE_RECORD("5004", "Bệnh nhân không có hồ sơ bệnh án đang hoạt động để ghi điểm UAS7", HttpStatus.BAD_REQUEST),
    UAS7_CYCLE_NOT_FOUND("5005", "Không tìm thấy chu kỳ theo dõi UAS7", HttpStatus.NOT_FOUND),
    UAS7_OVERLAPPING_CYCLE("5006", "Đã tồn tại chu kỳ theo dõi UAS7 đang hoạt động bị trùng thời gian", HttpStatus.CONFLICT),
    UAS7_INVALID_CYCLE_STATUS_TRANSITION("5007", "Chuyển đổi trạng thái chu kỳ UAS7 không hợp lệ", HttpStatus.BAD_REQUEST),
    EXAM_NOT_FOUND("5008", "Không tìm thấy phiên khám", HttpStatus.NOT_FOUND),

    // 6xxx - Media & attachment errors
    IMAGE_FILE_REQUIRED("6001", "Tệp hình ảnh là bắt buộc", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDS_LIMIT("6003", "Dung lượng tệp vượt quá giới hạn", HttpStatus.PAYLOAD_TOO_LARGE),
    ATTACHMENT_NOT_FOUND("6005", "Không tìm thấy tệp đính kèm", HttpStatus.NOT_FOUND),
    ATTACHMENT_IN_USE("6007", "Tệp đính kèm đang được sử dụng bởi ảnh tổn thương", HttpStatus.CONFLICT),

    // 7xxx - Lesion tracking & detail errors
    LESION_EPISODE_NOT_FOUND("7001", "Không tìm thấy đợt tổn thương da", HttpStatus.NOT_FOUND),
    LESION_NAME_CANNOT_BE_EMPTY("7004", "Tên đợt tổn thương không được để trống", HttpStatus.BAD_REQUEST),
    SYMPTOM_SCORE_OUT_OF_RANGE("7007", "Điểm số triệu chứng (ngứa, đau, rát) nằm ngoài khoảng 0-10", HttpStatus.BAD_REQUEST),
    EPISODE_HAS_CHILD_DATA("7009", "Đợt tổn thương vẫn còn vị trí hoặc hình ảnh đính kèm", HttpStatus.CONFLICT),
    LESION_DETAIL_NOT_FOUND("7010", "Không tìm thấy chi tiết tổn thương", HttpStatus.NOT_FOUND),

    // 80xx - Prescription errors
    PRESCRIPTION_ITEMS_EMPTY("8001", "Đơn thuốc phải có ít nhất một thuốc", HttpStatus.BAD_REQUEST),
    PRESCRIPTION_NOT_FOUND("8003", "Không tìm thấy đơn thuốc", HttpStatus.NOT_FOUND),
    PRESCRIPTION_ITEM_MEDICATION_CONFLICT("8004", "Mỗi thuốc trong đơn phải có đúng một trong hai trường ma_thuoc hoặc ten_thuoc_bo_sung", HttpStatus.BAD_REQUEST),
    MEDICATION_NOT_FOUND("8005", "Mã thuốc không tồn tại trong danh mục thuốc", HttpStatus.NOT_FOUND),

    // 81xx - Lab result errors
    LAB_RESULT_FIELD_TYPE_MISMATCH("8103", "Trường kết quả cận lâm sàng không khớp loại phiên khám", HttpStatus.BAD_REQUEST),
    LAB_RESULT_NOT_FOUND("8104", "Không tìm thấy kết quả cận lâm sàng", HttpStatus.NOT_FOUND),
    ASST_REQUIRES_POSITIVE_SERUM_TEST("8105", "Chỉ được nhập asst_duong_kinh_mm khi test_huyet_thanh_tu_than = Duong tinh", HttpStatus.BAD_REQUEST),

    // 82xx - Stop-medication instruction errors
    STOP_PURPOSE_OTHER_DESCRIPTION_REQUIRED("8201", "Mục đích ngừng thuốc là Khac thì bắt buộc nhập muc_dich_khac_ghi_ro", HttpStatus.BAD_REQUEST),
    STOP_PURPOSE_INVALID("8202", "Mục đích ngừng thuốc không hợp lệ, chỉ chấp nhận: Test huyet thanh tu than, Test kich thich, Khac", HttpStatus.BAD_REQUEST),

    // 83xx - Provocation test types & CIndU stimulation tests
    TEST_TYPE_NOT_FOUND("8301", "Không tìm thấy loại nghiệm pháp kích thích", HttpStatus.NOT_FOUND),
    TEST_TYPE_ALREADY_EXISTS("8302", "Tên loại nghiệm pháp kích thích đã tồn tại", HttpStatus.CONFLICT),
    TEST_TYPE_IN_USE("8303", "Loại nghiệm pháp đang được kết quả test tham chiếu", HttpStatus.CONFLICT),
    STIMULATION_TEST_RESULT_NOT_FOUND("8304", "Không tìm thấy dòng kết quả test kích thích", HttpStatus.NOT_FOUND),

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
