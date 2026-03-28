package com.crishof.springsecurity.util;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;

/**
 * Utility class for generating random verification codes and secure tokens.
 */
@UtilityClass
public class CodeGeneratorUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int VERIFICATION_CODE_LENGTH = 6;

    /**
     * Generates a numeric 6-digit verification code.
     *
     * @return six-digit code
     */
    public static String generateVerificationCode() {
        int upperBound = (int) Math.pow(10, VERIFICATION_CODE_LENGTH);
        int code = SECURE_RANDOM.nextInt(upperBound);
        return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", code);
    }
}
