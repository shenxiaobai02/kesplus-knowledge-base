package com.kes.common;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ResponseWrapper<T> {

    private String code;
    private String message;
    private T data;
    private boolean success;
    private long timestamp;
    private Map<String, Object> extra;

    private ResponseWrapper() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ResponseWrapper<T> success(T data) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setCode("SUCCESS");
        response.setMessage("操作成功");
        response.setData(data);
        response.setSuccess(true);
        return response;
    }

    public static <T> ResponseWrapper<T> success(T data, String message) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setCode("SUCCESS");
        response.setMessage(message);
        response.setData(data);
        response.setSuccess(true);
        return response;
    }

    public static <T> ResponseWrapper<T> success(String message) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setCode("SUCCESS");
        response.setMessage(message);
        response.setSuccess(true);
        return response;
    }

    public static <T> ResponseWrapper<T> error(String code, String message) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setCode(code);
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }

    public static <T> ResponseWrapper<T> error(String message) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setCode("ERROR");
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }

    public ResponseWrapper<T> addExtra(String key, Object value) {
        if (this.extra == null) {
            this.extra = new HashMap<>();
        }
        this.extra.put(key, value);
        return this;
    }

    public ResponseWrapper<T> setExtra(Map<String, Object> extra) {
        this.extra = extra;
        return this;
    }

    public static <T> ResponseWrapper<T> of(boolean success, String code, String message, T data) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setSuccess(success);
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}
