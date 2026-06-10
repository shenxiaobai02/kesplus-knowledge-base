package com.kes.util;

import com.kes.exception.BaseException;
import com.kes.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;

public class ValidationUtil {

    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void notEmpty(String str, String message) {
        if (!StringUtils.hasText(str)) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void isEmail(String email, String message) {
        if (!StringUtils.hasText(email) || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void isPhone(String phone, String message) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void isUuid(String uuid, String message) {
        if (!UuidUtil.isValid(uuid)) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void checkRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void checkRange(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }

    public static void checkRange(double value, double min, double max, String message) {
        if (value < min || value > max) {
            throw BaseException.of(ErrorCode.A_VALIDATION_ERROR, message);
        }
    }
}
