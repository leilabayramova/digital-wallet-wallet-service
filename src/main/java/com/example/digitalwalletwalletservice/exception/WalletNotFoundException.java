package com.example.digitalwalletwalletservice.exception;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(Long walletId) {
        super("Wallet not found with id: " + walletId);
    }
}
