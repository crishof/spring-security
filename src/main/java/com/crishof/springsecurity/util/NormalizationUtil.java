package com.crishof.springsecurity.util;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

/**
 * Utility class for input normalization.
 * Centralizes normalization logic reused by multiple services.
 */
@UtilityClass
public class NormalizationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    /**
     * Normalizes email by trimming, lowercasing, and validating basic format.
     *
     * @param email email to normalize
     * @return normalized email
     * @throws IllegalArgumentException if email is null, blank, or invalid
     */
    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        String normalized = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email format is invalid: " + normalized);
        }

        return normalized;
    }

    /**
     * Normalizes full name by trimming and capitalizing each word.
     *
     * @param fullName full name to normalize
     * @return normalized full name
     * @throws IllegalArgumentException if fullName is null or blank
     */
    public static String normalizeFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }

        String trimmed = fullName.trim();

        // Capitalize each word while preserving single spaces between tokens.
        String[] words = trimmed.split("\\s+");
        StringBuilder capitalized = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                capitalized.append(" ");
            }
            String word = words[i];
            if (!word.isEmpty()) {
                capitalized.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase());
            }
        }

        return capitalized.toString();
    }

}
