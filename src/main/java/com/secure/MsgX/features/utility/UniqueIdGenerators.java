package com.secure.MsgX.features.utility;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

import java.util.UUID;

public class UniqueIdGenerators {

    // Snowflake ID Generator
    public static class SnowflakeIdGenerator {
        private static final long EPOCH = 1609459200000L; // Custom epoch (e.g., 01-Jan-2021)
        private static final long SEQUENCE_BITS = 12L;
        private static final long MACHINE_ID_BITS = 10L;
        private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
        private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

        private long machineId;
        private long sequence = 0L;
        private long lastTimestamp = -1L;

        public SnowflakeIdGenerator(long machineId) {
            if (machineId < 0 || machineId > MAX_MACHINE_ID) {
                throw new IllegalArgumentException("Machine ID must be between 0 and " + MAX_MACHINE_ID);
            }
            this.machineId = machineId;
        }

        public synchronized long generateId() {
            long timestamp = System.currentTimeMillis() - EPOCH;

            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    timestamp = waitForNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0;
            }

            lastTimestamp = timestamp;

            return (timestamp << (MACHINE_ID_BITS + SEQUENCE_BITS)) | (machineId << SEQUENCE_BITS) | sequence;
        }

        private long waitForNextMillis(long lastTimestamp) {
            long timestamp;
            do {
                timestamp = System.currentTimeMillis() - EPOCH;
            } while (timestamp <= lastTimestamp);
            return timestamp;
        }
    }

    // ULID Generator
    public static class UlidGenerator {
        private static final long EPOCH = 0L; // Start timestamp (can use custom epoch)

        public static String generateUlid() {
            long timestamp = System.currentTimeMillis() - EPOCH;
            byte[] randomBytes = new byte[10];
            new SecureRandom().nextBytes(randomBytes);

            // Combine the timestamp and random part
            return String.format("%016x", timestamp) + bytesToHex(randomBytes);
        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        }
    }

    // UUID Generator
    public static class UuidGenerator {
        public static void generateUuid() {
            // Generate a random UUID
            UUID uuid = UUID.randomUUID();
            System.out.println("Generated UUID: " + uuid.toString());

            // Generate a UUID from a specific string (using MD5 hash as the basis)
            UUID nameBasedUUID = UUID.nameUUIDFromBytes("your-namespace".getBytes());
            System.out.println("Generated Name-based UUID: " + nameBasedUUID.toString());
        }
    }
//    public static void main(String[] args) {
//        // Example usage of Snowflake ID Generator
//        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(1L); // Machine ID
//        System.out.println("Generated Snowflake ID: " + snowflakeIdGenerator.generateId());
//
//        // Example usage of ULID Generator
//        System.out.println("Generated ULID: " + UlidGenerator.generateUlid());
//
//        // Example usage of UUID Generator
//        UuidGenerator.generateUuid();
//    }
}