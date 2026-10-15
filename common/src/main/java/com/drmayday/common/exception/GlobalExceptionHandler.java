package com.drmayday.common.exception;

import com.drmayday.common.dto.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<BaseResponse<Object>> handleAppException(AppException ex) {
        log.warn("AppException: {} - {}", ex.getErrorCode().getCode(), ex.getMessage());
        BaseResponse<Object> response = BaseResponse.error(
                ex.getErrorCode().getCode(),
                ex.getMessage()
        );
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("AccessDeniedException: {}", ex.getMessage());
        BaseResponse<Object> response = BaseResponse.error(
                ErrorCode.FORBIDDEN.getCode(),
                "Bạn không có quyền thực hiện thao tác này"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation error: {}", errors);
        BaseResponse<Object> response = BaseResponse.<Object>builder()
                .code(ErrorCode.BAD_REQUEST.getCode())
                .message("Dữ liệu đầu vào không hợp lệ")
                .data(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        BaseResponse<Object> response = BaseResponse.error(
                ErrorCode.BAD_REQUEST.getCode(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        BaseResponse<Object> response = BaseResponse.error(
                ErrorCode.INTERNAL_ERROR.getCode(),
                "Đã có lỗi hệ thống xảy ra"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
