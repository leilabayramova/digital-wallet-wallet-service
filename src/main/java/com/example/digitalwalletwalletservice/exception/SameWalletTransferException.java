package com.example.digitalwalletwalletservice.exception;

public class SameWalletTransferException extends RuntimeException {

    public SameWalletTransferException(Long walletId) {
        super("Cannot transfer to the same wallet: " + walletId);
    }
}
