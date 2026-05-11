package com.project.common.exception;

import com.project.common.api.Result;
import javax.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<Void> handleApiException(ApiException ex) {
    return Result.error(ex.getCode(), ex.getMessage());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    BindException.class,
    ConstraintViolationException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<Void> handleValidationException(Exception ex) {
    return Result.error(400, "参数校验失败");
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Result<Void> handleException(Exception ex) {
    return Result.error(500, "服务器内部错误");
  }
}

