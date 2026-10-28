package com.example.drmayday.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BaseResponse chuẩn hóa dữ liệu trả về cho tất cả các APIs trong hệ thống DrMayDay.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    @Builder.Default
    private String code = "1000";

    @Builder.Default
    private String message = "OK";

    private T data;

    public static <T> BaseResponse<T> ok() {
        return BaseResponse.<T>builder()
                .code("1000")
                .message("OK")
                .build();
    }

    public static <T> BaseResponse<T> ok(T data) {
        return BaseResponse.<T>builder()
                .code("1000")
                .message("OK")
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> success(T data) {
        return ok(data);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
                .code("1000")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> created(T data) {
        return BaseResponse.<T>builder()
                .code("1001")
                .message("Created successfully")
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> created(String message, T data) {
        return BaseResponse.<T>builder()
                .code("1001")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return BaseResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}