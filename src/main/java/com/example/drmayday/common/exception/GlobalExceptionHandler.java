package com.example.drmayday.common.exception;

import com.example.drmayday.common.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Bộ xử lý ngoại lệ tập trung (Global Exception Handler) chuẩn hóa format lỗi toàn hệ thống.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<BaseResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Ngoại lệ nghiệp vụ AppException [code={}, message={}]", errorCode.getCode(), ex.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.error(errorCode.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Lỗi kiểm tra tính hợp lệ tham số đầu vào: {}", ex.getMessage());
        FieldError fieldError = ex.getBindingResult().getFieldError();

        if (fieldError != null) {
            String defaultMessage = fieldError.getDefaultMessage();
            if ("2004".equals(defaultMessage)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.error(ErrorCode.INVALID_PHONE_NUMBER_FORMAT.getCode(),
                                ErrorCode.INVALID_PHONE_NUMBER_FORMAT.getMessage()));
            }
            if ("2003".equals(defaultMessage)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.error(ErrorCode.INVALID_PARAMETER_VALUE.getCode(),
                                ErrorCode.INVALID_PARAMETER_VALUE.getMessage()));
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.MISSING_REQUIRED_PARAMETER.getCode(),
                        ErrorCode.MISSING_REQUIRED_PARAMETER.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Lỗi sai kiểu tham số đầu vào: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.INVALID_PARAMETER_TYPE.getCode(),
                        ErrorCode.INVALID_PARAMETER_TYPE.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Từ chối truy cập do thiếu quyền hạn (AccessDenied): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error(ErrorCode.ADMIN_ROLE_REQUIRED.getCode(),
                        ErrorCode.ADMIN_ROLE_REQUIRED.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Lỗi xác thực AuthenticationException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error(ErrorCode.TOKEN_INVALID.getCode(),
                        ErrorCode.TOKEN_INVALID.getMessage()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Lỗi đọc định dạng request body: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.INVALID_PARAMETER_VALUE.getCode(),
                        ErrorCode.INVALID_PARAMETER_VALUE.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Lỗi vi phạm toàn vẹn dữ liệu CSDL: {}", ex.getMessage(), ex);
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (msg.contains("duplicate key") || msg.contains("unique constraint") || msg.contains("violates unique") || msg.contains("uq_") || (msg.contains("unique") && msg.contains("constraint"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(BaseResponse.error(ErrorCode.USER_PHONE_ALREADY_EXISTS.getCode(),
                            ErrorCode.USER_PHONE_ALREADY_EXISTS.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.INVALID_PARAMETER_VALUE.getCode(),
                        ErrorCode.INVALID_PARAMETER_VALUE.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Lỗi hệ thống chưa được kiểm soát: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
