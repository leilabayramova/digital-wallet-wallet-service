package com.example.digitalwalletwalletservice.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(Long walletId) {
        super("Insufficient balance in wallet: " + walletId);
    }
}
