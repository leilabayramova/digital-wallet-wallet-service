package com.example.digitalwalletwalletservice.exception;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(Long sourceWalletId, Long targetWalletId) {
        super("Currency mismatch between wallet %d and wallet %d".formatted(sourceWalletId, targetWalletId));
    }
}
