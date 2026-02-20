package org.example.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(boolean success, String message, Object data) {
    public static ApiResponse ok(String message, Object data) {
        return new ApiResponse(true, message, data);
    }

    public static ApiResponse fail(String message) {
        return new ApiResponse(false, message, null);
    }

    public static ApiResponse fail(String message, Object data) {
        return new ApiResponse(false, message, data);
    }
}
