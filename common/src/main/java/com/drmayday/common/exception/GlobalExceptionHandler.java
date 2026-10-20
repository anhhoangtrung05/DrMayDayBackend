package com.drmayday.common.exception;

import com.drmayday.common.dto.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Bộ xử lý ngoại lệ tập trung (Global Exception Handler) chuẩn hóa format lỗi toàn hệ thống.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<BaseResponse<Object>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Ngoại lệ nghiệp vụ AppException [code={}, message={}]", errorCode.getCode(), ex.getMessage());
        BaseResponse<Object> response = BaseResponse.error(errorCode.getCode(), ex.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Lỗi kiểm tra tính hợp lệ tham số đầu vào: {}", ex.getMessage());
        FieldError fieldError = ex.getBindingResult().getFieldError();

        if (fieldError != null) {
            String defaultMessage = fieldError.getDefaultMessage();
            if ("2004".equals(defaultMessage)) {
                BaseResponse<Object> response = BaseResponse.error(
                        ErrorCode.INVALID_PHONE_NUMBER_FORMAT.getCode(),
                        ErrorCode.INVALID_PHONE_NUMBER_FORMAT.getMessage()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            if ("2003".equals(defaultMessage)) {
                BaseResponse<Object> response = BaseResponse.error(
                        ErrorCode.INVALID_PARAMETER_VALUE.getCode(),
                        ErrorCode.INVALID_PARAMETER_VALUE.getMessage()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }

        // Trường hợp trả về danh sách chi tiết các field lỗi nếu cần
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }

        BaseResponse<Object> response = BaseResponse.<Object>builder()
                .code(ErrorCode.MISSING_REQUIRED_PARAMETER.getCode())
                .message(ErrorCode.MISSING_REQUIRED_PARAMETER.getMessage())
                .data(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Lỗi sai kiểu tham số đầu vào: {}", ex.getMessage());
        BaseResponse<Object> response = BaseResponse.error(
                ErrorCode.INVALID_PARAMETER_TYPE.getCode(),
                ErrorCode.INVALID_PARAMETER_TYPE.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Từ chối truy cập do thiếu quyền hạn (AccessDenied): {}", ex.getMessage());
        BaseResponse<Object> response = BaseResponse.error(
                ErrorCode.ADMIN_ROLE_REQUIRED.getCode(),
                ErrorCode.ADMIN_ROLE_REQUIRED.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Lỗi xác thực AuthenticationException: {}", ex.getMessage());
        BaseResponse<Object> response = BaseResponse.error(
                ErrorCode.TOKEN_INVALID.getCode(),
                ErrorCode.TOKEN_INVALID.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Lỗi đọc định dạng request body: {}", ex.getMessage());
        BaseResponse<Object> response = BaseResponse.error(
                ErrorCode.INVALID_PARAMETER_VALUE.getCode(),
                ErrorCode.INVALID_PARAMETER_VALUE.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Object>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Lỗi vi phạm toàn vẹn dữ liệu CSDL: {}", ex.getMessage());
        String msg = ex.