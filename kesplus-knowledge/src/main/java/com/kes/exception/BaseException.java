package com.kes.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String message;
    private final Object data;

    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.data = null;
    }

    public BaseException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
    }

    public BaseException(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
        this.message = message;
        this.data = null;
    }

    public String getCode() {
        return errorCode.name();
    }

    public static BaseException of(ErrorCode errorCode, String message) {
        return new BaseException(errorCode, message);
    }

    public static BaseException of(ErrorCode errorCode, String message, Object data) {
        return new BaseException(errorCode, message, data);
    }
}
