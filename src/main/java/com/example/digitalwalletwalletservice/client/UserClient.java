package com.example.digitalwalletwalletservice.client;

import com.example.digitalwalletwalletservice.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "digital-wallet-user-service",
        url = "${clients.user-service.url}"
)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserResponseDto getUserById(@PathVariable Long id);
}