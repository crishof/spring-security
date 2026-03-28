package com.crishof.springsecurity.util;

import com.crishof.springsecurity.exception.BusinessException;
import lombok.experimental.UtilityClass;

/**
 * Utility class for validating business-level input constraints.
 */
@UtilityClass
public class ValidationUtil {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 72;
    public static final int VERIFICATION_CODE_LENGTH = 6;
    public static final int FULL_NAME_MAX_LENGTH = 120;
    public static final int EMAIL_MAX_LENGTH = 150;

    /**
     * Validates password against security requirements.
     *
     * @param password password to validate
     * @throws BusinessException when requirements are not met
     */
    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("Password cannot be null or empty");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(
                    "Password must not exceed " + MAX_PASSWORD_LENGTH + " characters");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new BusinessException("Password must contain at least one digit");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new BusinessException("Password must contain at least one special character");
        }
    }

    /**
     * Validates full name.
     *
     * @param fullName full name to validate
     * @throws BusinessException when value is invalid
     */
    public static void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("Full name cannot be null or empty");
        }

        if (fullName.length() > FULL_NAME_MAX_LENGTH) {
            throw new BusinessException(
                    "Full name must not exceed " + FULL_NAME_MAX_LENGTH + " characters");
        }

        if (fullName.length() < 2) {
            throw new BusinessException("Full name must be at least 2 characters");
        }
    }

    /**
     * Validates email.
     *
     * @param email email to validate
     * @throws BusinessException when value is invalid
     */
    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email cannot be null or empty");
        }

        if (email.length() > EMAIL_MAX_LENGTH) {
            throw new BusinessException(
                    "Email must not exceed " + EMAIL_MAX_LENGTH + " characters");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BusinessException("Email format is invalid");
        }
    }

    /**
     * Validates verification code.
     *
     * @param code code to validate
     * @throws BusinessException when value is invalid
     */
    public static void validateVerificationCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Verification code cannot be null or empty");
        }

        if (code.length() != VERIFICATION_CODE_LENGTH) {
            throw new BusinessException(
                    "Verification code must be " + VERIFICATION_CODE_LENGTH + " digits");
        }

        if (!code.matches("\\d+")) {
            throw new BusinessException("Verification code must contain only digits");
        }
    }

    /**
     * Validates that two values are equal.
     *
     * @param value1 first value
     * @param value2 second value
     * @param fieldName field label used in the error message
     * @throws BusinessException when values differ
     */
    public static void validateEqualityOf(String value1, String value2, String fieldName) {
        if (value1 == null || !value1.equals(value2)) {
            throw new BusinessException(fieldName + " values do not match");
        }
    }
}
