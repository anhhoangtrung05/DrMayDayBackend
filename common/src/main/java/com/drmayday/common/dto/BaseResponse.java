package com.drmayday.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * BaseResponse chuẩn hóa dữ liệu trả về cho tất cả các APIs trong hệ thống DrMayDay.
 * Tuân thủ cấu trúc đặc tả:
 * {
 *   "code": "1000",
 *   "message": "OK",
 *   "data": { ... }
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    private String code;
    private String message;
    private T data;

    public BaseResponse() {
    }

    public BaseResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>("1000", "OK", data);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>("1000", message, data);
    }

    public static <T> BaseResponse<T> created(T data) {
        return new BaseResponse<>("1001", "Created successfully", data);
    }

    public static <T> BaseResponse<T> created(String message, T data) {
        return new BaseResponse<>("1001", message, data);
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(code, message, null);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private String code;
        private String message;
        private T data;

        public Builder<T> code(String code) {
            this.code = code;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public BaseResponse<T> build() {
            return new BaseResponse<>(code, message, data);
        }
    }
}
