package com.secure.MsgX.features.utility;

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

    public byte[] encryptContent(String plainText,
                                 List<String> passkeys,
                                 String salt,
                                 EncryptionAlgo algorithm) throws GlobalMsgXExceptions {
        try {
            // Use PBKDF2 for proper key derivation
            SecretKey secretKey = deriveKey(passkeys, salt, algorithm);
            byte[] iv = generateIV();
            lastGeneratedIV = iv;

            Cipher cipher = Cipher.getInstance(algorithm.getTransformation(), "BC");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new GlobalMsgXExceptions("Encryption failed: " + e.getMessage(), e);
        }
    }

    public String decryptContent(byte[] cipherText,
                                 List<String> passkeys,
                                 String salt,
                                 byte[] iv,
                                 EncryptionAlgo algorithm) throws GlobalMsgXExceptions {
        try {
            SecretKey secretKey = deriveKey(passkeys, salt, algorithm);
            Cipher cipher = Cipher.getInstance(algorithm.getTransformation(), "BC");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new GlobalMsgXExceptions("Decryption failed: " + e.getMessage(), e);
        }
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