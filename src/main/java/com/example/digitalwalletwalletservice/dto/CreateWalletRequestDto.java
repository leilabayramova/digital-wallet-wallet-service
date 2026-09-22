package com.example.digitalwalletwalletservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateWalletRequestDto {

    @NotNull(message = "User id cannot be null")
    private Long userId;

    @NotBlank(message = "Currency cannot be empty")
    @Size(min = 3, max = 3, message = "Currency must contain exactly 3 characters")
    private String currency;
}