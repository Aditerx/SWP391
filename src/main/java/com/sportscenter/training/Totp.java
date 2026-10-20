package com.sportscenter.training;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

final class Totp {
    private Totp() { }
    static byte[] randomSecret() {
        byte[] secret = new byte[20];
        new java.security.SecureRandom().nextBytes(secret);
        return secret;
    }
    static String encodeSecret(byte[] secret) { return Base64.getEncoder().encodeToString(secret); }
    static String code(byte[] secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) { throw new IllegalStateException("Unable to generate check-in code", e); }
    }
    static Long matchingStep(byte[] secret, String supplied, long currentStep) {
        if (supplied == null || !supplied.matches("\\d{6}")) return null;
        for (long step = currentStep - 1; step <= currentStep + 1; step++) {
            if (java.security.MessageDigest.isEqual(code(secret, step).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    supplied.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) return step;
        }
        return null;
    }
}
