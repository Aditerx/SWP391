package com.sportscenter.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The existing SQL seed stores SHA2_256 hex values generated from an NVARCHAR
 * literal. SQL Server hashes that value as UTF-16LE, so the primary comparison
 * must use UTF-16LE. BCrypt and UTF-8 remain compatibility paths.
 */
public class LegacyPasswordEncoder implements PasswordEncoder {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$")
                || encodedPassword.startsWith("$2y$")) {
            return bcrypt.matches(rawPassword, encodedPassword);
        }
        String expected = encodedPassword.trim();
        byte[] stored = expected.toUpperCase().getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(sha256(rawPassword.toString(), StandardCharsets.UTF_16LE)
                        .getBytes(StandardCharsets.US_ASCII), stored)
                // Keep compatibility with any older seed generated from UTF-8.
                || MessageDigest.isEqual(sha256(rawPassword.toString(), StandardCharsets.UTF_8)
                        .getBytes(StandardCharsets.US_ASCII), stored);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return encodedPassword == null || !encodedPassword.startsWith("$2");
    }

    private String sha256(String value) {
        return sha256(value, StandardCharsets.UTF_16LE);
    }

    private String sha256(String value, java.nio.charset.Charset charset) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(charset));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                hex.append(String.format("%02X", current));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
