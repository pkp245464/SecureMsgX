package com.secure.MsgX.features.utility.commonUtil;

import jakarta.servlet.http.HttpServletRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

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


    public static String extractAndHashIp(HttpServletRequest request) {
        return hashIpAddress(extractClientIp(request));
    }
}
