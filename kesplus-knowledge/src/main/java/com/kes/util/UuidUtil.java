package com.kes.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

public class UuidUtil {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ALPHABET_LENGTH = ALPHABET.length();

    public static String create() {
        return UUID.randomUUID().toString();
    }

    public static String createShort() {
        UUID uuid = UUID.randomUUID();
        return encodeUuid(uuid);
    }

    public static String createShort(String prefix) {
        return prefix + "_" + createShort();
    }

    public static String createShortWithTimestamp() {
        long timestamp = System.currentTimeMillis();
        String timestampStr = Long.toString(timestamp, 36);
        String randomPart = encodeUuid(UUID.randomUUID()).substring(0, 8);
        return timestampStr + randomPart;
    }

    private static String encodeUuid(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        return encodeLong(msb) + encodeLong(lsb);
    }

    private static String encodeLong(long value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt((int) (value % ALPHABET_LENGTH)));
            value = value / ALPHABET_LENGTH;
        }
        return sb.reverse().toString();
    }

    public static String generateMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public static String generateSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static boolean isValid(String uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
