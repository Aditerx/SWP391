package com.sportscenter.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class TemporaryPasswordGeneratorTest {
    private final TemporaryPasswordGenerator generator =
            new TemporaryPasswordGenerator(new BCryptPasswordEncoder());

    @Test
    void generatesTenCharacterMixedPasswordAndBcryptHash() {
        String password = generator.generate();

        assertEquals(10, password.length());
        assertTrue(password.matches(".*[A-Z].*"));
        assertTrue(password.matches(".*[a-z].*"));
        assertTrue(password.matches(".*[0-9].*"));
        String hash = generator.hash(password);
        assertTrue(hash.startsWith("$2"));
        assertTrue(new BCryptPasswordEncoder().matches(password, hash));
    }
}
