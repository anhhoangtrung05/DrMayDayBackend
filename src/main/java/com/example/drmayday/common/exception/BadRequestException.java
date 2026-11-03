package com.example.drmayday.common.exception;

public class BadRequestException extends AppException {
    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}
