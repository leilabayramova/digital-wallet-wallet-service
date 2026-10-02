package com.example.digitalwalletwalletservice.util;

import java.security.SecureRandom;

public final class WalletNumberGenerator {

    private static final String PREFIX = "WLT";
    private static final int DIGITS_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private WalletNumberGenerator() {
    }

    public static String generate() {
        StringBuilder digits = new StringBuilder(DIGITS_LENGTH);
        for (int i = 0; i < DIGITS_LENGTH; i++) {
            digits.append(RANDOM.nextInt(10));
        }
        return PREFIX + digits;
    }
}
