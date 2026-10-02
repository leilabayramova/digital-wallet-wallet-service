package com.example.digitalwalletwalletservice.client;

import com.example.digitalwalletwalletservice.exception.UserNotFoundException;
import com.example.digitalwalletwalletservice.exception.UserServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Mock
    private UserClient userClient;

    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        userServiceClient = new UserServiceClient(userClient, new Resilience4JCircuitBreakerFactory(
                CircuitBreakerRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(), null));
    }

    @Test
    void verifyUserExists_shouldNotThrow_whenUserExists() {
        assertThatCode(() -> userServiceClient.verifyUserExists(1L)).doesNotThrowAnyException();
    }

    @Test
    void verifyUserExists_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        doThrow(mock(FeignException.NotFound.class)).when(userClient).getUserById(99L);

        assertThatThrownBy(() -> userServiceClient.verifyUserExists(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void verifyUserExists_shouldThrowUserServiceUnavailableException_whenCallFailsForOtherReasons() {
        doThrow(new RuntimeException("connection refused")).when(userClient).getUserById(5L);

        assertThatThrownBy(() -> userServiceClient.verifyUserExists(5L))
                .isInstanceOf(UserServiceUnavailableException.class);
    }
}
