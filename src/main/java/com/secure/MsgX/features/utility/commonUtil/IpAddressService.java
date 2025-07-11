package com.secure.MsgX.features.utility.commonUtil;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;

public class IpAddressService {

    private static final String STATIC_SALT = "MsgX$2025!Salt@Value";

    public static String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (Objects.isNull(ip) || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (Objects.nonNull(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public static String hashIpAddress(String ipAddress) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String inputWithSalt = ipAddress + STATIC_SALT;
            byte[] hash = digest.digest(inputWithSalt.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static String shuffleAndShiftHash(String input, String entropySeed) {
        String base64 = Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
        char[] chars = base64.toCharArray();

        long seed = 0;
        for (byte b : entropySeed.getBytes(StandardCharsets.UTF_8)) {
            seed = seed * 31 + b;
        }
        Random random = new Random(seed);

        final int MIN = 32, MAX = 126, RANGE = MAX - MIN + 1;
        for (int i = 0; i < chars.length; i++) {
            int shift = random.nextInt(5, 15);
            int base = chars[i] - MIN;
            int shifted = random.nextBoolean()
                    ? (base + shift) % RANGE
                    : (base - shift + RANGE) % RANGE;
            chars[i] = (char) (shifted + MIN);
        }

        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    public static String extractAndHashIp(HttpServletRequest request) {
        return hashIpAddress(extractClientIp(request));
    }
}
