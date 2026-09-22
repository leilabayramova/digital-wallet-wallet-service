package com.example.digitalwalletwalletservice.dto;

import com.example.digitalwalletwalletservice.enums.WalletStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponseDto {

    private Long id;
    private Long userId;
    private String walletNumber;
    private BigDecimal balance;
    private String currency;
    private WalletStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}