package com.delivery.auth.service;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class EmailNormalizer {
    private static final String UNKNOWN_EMAIL = "unknown";

    public String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizeOrUnknown(String email) {
        return email == null ? UNKNOWN_EMAIL : normalize(email);
    }
}
