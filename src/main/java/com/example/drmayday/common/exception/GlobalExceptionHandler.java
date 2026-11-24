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
 * Centralized exception handler that standardizes error responses across the system.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<BaseResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("AppException [code={}, message={}]", errorCode.getCode(), ex.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.error(errorCode.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Input validation failed: {}", ex.getMessage());
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
        log.warn("Input parameter type mismatch: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.INVALID_PARAMETER_TYPE.getCode(),
                        ErrorCode.INVALID_PARAMETER_TYPE.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied due to insufficient permissions: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error(ErrorCode.ADMIN_ROLE_REQUIRED.getCode(),
                        ErrorCode.ADMIN_ROLE_REQUIRED.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error(ErrorCode.TOKEN_INVALID.getCode(),
                        ErrorCode.TOKEN_INVALID.getMessage()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Failed to read request body: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.INVALID_PARAMETER_VALUE.getCode(), describeUnreadableBody(ex)));
    }

    /**
     * Builds a specific message for an unreadable body: names the offending field when Jackson
     * reports one (e.g. "items[0].so_luong"), otherwise says whether the JSON is malformed or missing.
     */
    static String describeUnreadableBody(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        tools.jackson.core.JacksonException jackson = findCause(ex, tools.jackson.core.JacksonException.class);
        if (jackson == null) {
            return "Thiếu hoặc không đọc được nội dung request body";
        }
        if (jackson instanceof tools.jackson.core.exc.StreamReadException) {
            return "Nội dung request body không phải JSON hợp lệ";
        }
        String field = toFieldPath(jackson.getPath());
        if (field.isEmpty()) {
            return ErrorCode.INVALID_PARAMETER_VALUE.getMessage();
        }
        if (jackson instanceof tools.jackson.databind.exc.UnrecognizedPropertyException) {
            return "Trường không được hỗ trợ: " + field;
        }
        return "Giá trị của trường " + field + " không đúng kiểu dữ liệu";
    }

    private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return null;
    }

    /** Joins Jackson's reference path into "a.b[0].c", using the JSON property names. */
    private static String toFieldPath(java.util.List<tools.jackson.core.JacksonException.Reference> path) {
        StringBuilder sb = new StringBuilder();
        for (tools.jackson.core.JacksonException.Reference ref : path) {
            if (ref.getPropertyName() != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Database integrity constraint violation: {}", ex.getMessage(), ex);
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
        log.error("Unhandled system exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
