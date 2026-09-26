package com.sportscenter.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyPasswordEncoderTest {

    private final LegacyPasswordEncoder encoder = new LegacyPasswordEncoder();

    @Test
    void matchesSqlServerSha256SeedPassword() {
        String sqlServerHash = "4D7C635495F41FD7F9B9027BB64142569A1851F984EF405EBA77D161969AF866";

        assertTrue(encoder.matches("12345678", sqlServerHash));
        assertFalse(encoder.matches("wrong-password", sqlServerHash));
    }
}
