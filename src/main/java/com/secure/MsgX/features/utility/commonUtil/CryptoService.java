package com.secure.MsgX.features.utility.commonUtil;

import com.secure.MsgX.core.enums.EncryptionAlgo;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import lombok.Getter;
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
            SecretKey secretKey = deriveKey(passkeys, salt, algorithm);
            byte[] iv = generateIV();
            lastGeneratedIV = iv;

            Cipher cipher = Cipher.getInstance(algorithm.getTransformation(), "BC");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            // CHANGED: Convert to Base64 string before returning
            return Base64.getEncoder().encodeToString(encryptedBytes);
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
            // CHANGED: Decode Base64 strings to bytes
            byte[] cipherText = Base64.getDecoder().decode(base64CipherText);
            byte[] iv = Base64.getDecoder().decode(base64Iv);

            SecretKey secretKey = deriveKey(passkeys, salt, algorithm);
            Cipher cipher = Cipher.getInstance(algorithm.getTransformation(), "BC");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new GlobalMsgXExceptions("Decryption failed: " + e.getMessage(), e);
        }
    }

    public String getLastGeneratedIVAsBase64() {
        return Base64.getEncoder().encodeToString(lastGeneratedIV);
    }

    private SecretKey deriveKey(List<String> passkeys, String salt, EncryptionAlgo algorithm) throws Exception {
        String combinedKey = String.join("", passkeys);
        byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);

        // Use PBKDF2 for key derivation
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
                combinedKey.toCharArray(),
                saltBytes,
                PBKDF2_ITERATIONS,
                algorithm.getKeyLength() * 8
        );

        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, algorithm.getAlgorithmName());
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