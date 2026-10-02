package com.example.digitalwalletwalletservice.util;

import com.example.digitalwalletwalletservice.dto.TransferRequestDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class TransferRequestHasher {

    private TransferRequestHasher() {
    }

    public static String hash(TransferRequestDto requestDto) {
        String payload = requestDto.getSourceWalletId() + "|" + requestDto.getTargetWalletId() + "|" +
                requestDto.getAmount().stripTrailingZeros().toPlainString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
