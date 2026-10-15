package com.drmayday.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    SUCCESS("1000", "OK", HttpStatus.OK),
    BAD_REQUEST("1001", "Bad Request", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("1002", "Resource Not Found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("1003", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("1004", "Forbidden", HttpStatus.FORBIDDEN),
    DUPLICATE_RESOURCE("1005", "Duplicate Resource", HttpStatus.CONFLICT),
    INTERNAL_ERROR("9999", "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
