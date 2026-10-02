package com.example.digitalwalletwalletservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI walletServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Wallet - Wallet Service")
                        .description("Wallet management, money transfers and transaction history")
                        .version("v1"));
    }
}
