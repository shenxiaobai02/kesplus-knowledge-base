package com.kes.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final String code;
    private final String message;
    private final Object data;

    public BaseException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = null;
    }

    public BaseException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseException(String message) {
        super(message);
        this.code = "ERROR";
        this.message = message;
        this.data = null;
    }

    public static BaseException of(String code, String message) {
        return new BaseException(code, message);
    }

    public static BaseException of(String code, String message, Object data) {
        return new BaseException(code, message, data);
    }
}
