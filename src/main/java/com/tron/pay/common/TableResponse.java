package com.tron.pay.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableResponse<T> {
    private int code;
    private String message;
    private long count;
    private T data;

    public static <T> TableResponse<T> success(long count, T data) {
        return new TableResponse<>(0, "操作成功", count, data);
    }

    public static <T> TableResponse<T> fail(String message) {
        return new TableResponse<>(201, message, 0, null);
    }
}
