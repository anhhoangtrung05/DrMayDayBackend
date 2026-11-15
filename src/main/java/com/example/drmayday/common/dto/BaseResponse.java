package com.example.drmayday.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardizes response data for all APIs in the DrMayDay system.
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
    private String message = "Thành công";

    private T data;

    public static <T> BaseResponse<T> ok() {
        return BaseResponse.<T>builder()
                .code("1000")
                .message("Thành công")
                .build();
    }

    public static <T> BaseResponse<T> ok(T data) {
        return BaseResponse.<T>builder()
                .code("1000")
                .message("Thành công")
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
                .message("Tạo thành công")
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