package com.tron.pay.common;

import lombok.Data;

import java.util.Map;
import java.util.HashMap;
@Data
public class ApiResponse {
  private int code;
  private String msg;
  private Object data;

  public ApiResponse(int code, String message, Object data) {
    this.code = code;
    this.msg = message;
    this.data = data;
  }


  public static ApiResponse success() {
    return new ApiResponse(200, "操作成功", null);
  }

  public static ApiResponse success(String msg) {
    return new ApiResponse(200, msg, null);
  }

  public static ApiResponse success(String msg,Object data) {
    return new ApiResponse(200, msg, data);
  }

  public static ApiResponse success(Object data) {
    return new ApiResponse(200, "操作成功", data);
  }

  public static ApiResponse fail(String msg) {
    return new ApiResponse(-1, msg, null);
  }

  public static ApiResponse fail(int code, String msg) {
    return new ApiResponse(code, msg, null);
  }
}