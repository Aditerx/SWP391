package com.sportscenter.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TemporaryPasswordGenerator {
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = UPPER + LOWER + DIGITS;
    private final SecureRandom random = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    public TemporaryPasswordGenerator(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String generate() {
        List<Character> characters = new ArrayList<>();
        characters.add(pick(UPPER));
        characters.add(pick(LOWER));
        characters.add(pick(DIGITS));
        while (characters.size() < 10) characters.add(pick(ALL));
        Collections.shuffle(characters, random);
        StringBuilder value = new StringBuilder(10);
        characters.forEach(value::append);
        return value.toString();
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
