package com.project.common.api;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {
  private int code;
  private String message;
  private T data;
  private long timestamp;

  public static <T> Result<T> success(T data) {
    return new Result<>(0, "OK", data, Instant.now().toEpochMilli());
  }

  public static <T> Result<T> error(int code, String message) {
    return new Result<>(code, message, null, Instant.now().toEpochMilli());
  }
}

