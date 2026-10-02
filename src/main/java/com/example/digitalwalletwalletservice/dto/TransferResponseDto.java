package com.example.digitalwalletwalletservice.dto;

import com.example.digitalwalletwalletservice.enums.TransferStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDto {

    private Long id;
    private Long sourceWalletId;
    private Long targetWalletId;
    private BigDecimal amount;
    private String currency;
    private TransferStatus status;
    private LocalDateTime createdAt;
}
