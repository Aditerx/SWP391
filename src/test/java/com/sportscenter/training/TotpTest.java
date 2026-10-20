package com.sportscenter.training;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TotpTest {
    @Test
    void followsRfc6238HmacSha1SixDigitVectorAt59Seconds() {
        byte[] secret = "12345678901234567890".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals("287082", Totp.code(secret, 1));
        assertEquals(Long.valueOf(1), Totp.matchingStep(secret, "287082", 1));
    }

    @Test
    void matchingStepAcceptsOneStepBehind() {
        byte[] secret = "12345678901234567890".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals(999L, Totp.matchingStep(secret, Totp.code(secret, 999), 1000));
    }

    @Test
    void matchingStepAcceptsOneStepAhead() {
        byte[] secret = "12345678901234567890".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals(1001L, Totp.matchingStep(secret, Totp.code(secret, 1001), 1000));
    }

    @Test
    void matchingStepRejectsTwoStepsAway() {
        byte[] secret = "12345678901234567890".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertNull(Totp.matchingStep(secret, Totp.code(secret, 998), 1000));
    }
}
