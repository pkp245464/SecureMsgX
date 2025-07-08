package com.secure.MsgX.core.enums;

import lombok.Getter;

@Getter
public enum EncryptionAlgo {
    AES_256("AES/GCM/NoPadding", 32, "AES"),
    CHACHA20("ChaCha20-Poly1305", 32, "ChaCha20"),
    TWOFISH("Twofish/GCM/NoPadding", 32, "Twofish");

    private final String transformation;
    private final int keyLength;
    private final String algorithmName;

    EncryptionAlgo(String transformation, int keyLength, String algorithmName) {
        this.transformation = transformation;
        this.keyLength = keyLength;
        this.algorithmName = algorithmName;
    }
}
