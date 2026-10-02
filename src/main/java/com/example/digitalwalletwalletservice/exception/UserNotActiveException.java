package com.example.digitalwalletwalletservice.exception;

public class UserNotActiveException extends RuntimeException {

    public UserNotActiveException(Long userId) {
        super("User with id %d is not active".formatted(userId));
    }
}
