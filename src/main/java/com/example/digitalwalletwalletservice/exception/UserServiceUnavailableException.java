package com.example.digitalwalletwalletservice.exception;

public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(Long userId, Throwable cause) {
        super("User service is currently unavailable, cannot verify user: " + userId, cause);
    }
}
