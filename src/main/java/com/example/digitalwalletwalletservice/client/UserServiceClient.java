package com.example.digitalwalletwalletservice.client;

import com.example.digitalwalletwalletservice.dto.UserResponseDto;
import com.example.digitalwalletwalletservice.exception.UserNotActiveException;
import com.example.digitalwalletwalletservice.exception.UserNotFoundException;
import com.example.digitalwalletwalletservice.exception.UserServiceUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private static final String CIRCUIT_BREAKER_ID = "userService";

    private final UserClient userClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public void verifyUserExists(Long userId) {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_ID);

        circuitBreaker.run(
                () -> {
                    UserResponseDto user;
                    try {
                        user = userClient.getUserById(userId);
                    } catch (FeignException.NotFound ex) {
                        throw new UserNotFoundException(userId);
                    }

                    if (!user.isActive()) {
                        throw new UserNotActiveException(userId);
                    }

                    return null;
                },
                throwable -> handleFailure(userId, throwable)
        );
    }

    private Void handleFailure(Long userId, Throwable throwable) {
        if (throwable instanceof UserNotFoundException || throwable instanceof UserNotActiveException) {
            throw (RuntimeException) throwable;
        }

        log.error("User service call failed for userId={}", userId, throwable);
        throw new UserServiceUnavailableException(userId, throwable);
    }
}
