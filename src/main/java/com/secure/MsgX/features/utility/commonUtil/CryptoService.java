package com.secure.MsgX.features.utility.commonUtil;

import com.secure.MsgX.core.enums.EncryptionAlgo;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CryptoService {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 100000;

    @Getter
    private byte[] lastGeneratedIV;
    private final PasswordEncoder passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public String encryptContent(String plainText,
                                 List<String> passkeys,
                                 String salt,
                                 EncryptionAlgo algorithm) throws GlobalMsgXExceptions {
        try {
            // Normalize inputs
            String normalizedSalt = salt.trim();
            List<String> normalizedPasskeys = passkeys.stream()
                    .map(String::trim)
                    .sorted()
                    .toList();

            log.info("CryptoService::EncryptContent - Encrypting with salt: '{}' and passkeys: {}", normalizedSalt, normalizedPasskeys);

            // Key derivation
            String keyInput = String.join("|", normalizedPasskeys) + "|" + normalizedSalt;
            byte[] saltBytes = normalizedSalt.getBytes(StandardCharsets.UTF_8);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                    keyInput.toCharArray(),
                    saltBytes,
                    PBKDF2_ITERATIONS,
                    algorithm.getKeyLength() * 8
            );

            SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            // Encryption
            byte[] iv = generateIV();
            lastGeneratedIV = iv;

            Cipher cipher = Cipher.getInstance(algorithm.getTransformation(), "BC");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new GlobalMsgXExceptions("Encryption failed: " + e.getMessage(), e);
        }
    }

    // CHANGED: Parameters changed to accept Base64 strings
    public String decryptContent(String base64CipherText,
                                 List<String> passkeys,
                                 String salt,
                                 String base64Iv,
                                 EncryptionAlgo algorithm) throws GlobalMsgXExceptions {
        try {
            // Normalize exactly as during encryption
            String normalizedSalt = salt.trim();
            List<String> normalizedPasskeys = passkeys.stream()
                    .map(String::trim)
                    .sorted()
                    .toList();

            log.info("CryptoService::DecryptContent - Decrypting with salt: '{}' and passkeys: {}", normalizedSalt, normalizedPasskeys);

            // Key derivation (must match encryption exactly)
            String keyInput = String.join("|", normalizedPasskeys) + "|" + normalizedSalt;
            byte[] saltBytes = normalizedSalt.getBytes(StandardCharsets.UTF_8);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                    keyInput.toCharArray(),
                    saltBytes,
                    PBKDF2_ITERATIONS,
                    algorithm.getKeyLength() * 8
            );

            SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            // Decryption
            byte[] iv = Base64.getDecoder().decode(base64Iv);
            byte[] cipherText = Base64.getDecoder().decode(base64CipherText);

            Cipher cipher = Cipher.getInstance(algorithm.getTransformation(), "BC");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            log.error("CryptoService::DecryptContent - Decryption failed. Error: {}", e.getMessage(), e);
            throw new GlobalMsgXExceptions("Decryption failed. Please verify: " +
                    "1. The exact passkey is correct (including case and whitespace)\n" +
                    "2. The ticket hasn't been corrupted\n" +
                    "Technical details: " + e.getMessage());
        }
    }

    public String getLastGeneratedIVAsBase64() {
        return Base64.getEncoder().encodeToString(lastGeneratedIV);
    }

    private SecretKey deriveKey(List<String> passkeys, String salt, EncryptionAlgo algorithm) throws Exception {
        // Create a consistent key derivation input
        String combinedInput = passkeys.stream()
                .map(String::trim)
                .sorted()
                .collect(Collectors.joining("::")) + "||" + salt;

        log.debug("Key derivation input: {}", combinedInput);

        byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
                combinedInput.toCharArray(),
                saltBytes,
                PBKDF2_ITERATIONS,
                algorithm.getKeyLength() * 8
        );

        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public String hashPasskey(String passkey) {
        return passwordEncoder.encode(passkey);
    }

    public boolean verifyPasskey(String rawPasskey, String hashedPasskey) {
        return passwordEncoder.matches(rawPasskey, hashedPasskey);
    }

    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
    }

    private byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }
}