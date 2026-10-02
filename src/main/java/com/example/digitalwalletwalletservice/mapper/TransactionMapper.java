package com.example.digitalwalletwalletservice.mapper;

import com.example.digitalwalletwalletservice.dto.TransactionResponseDto;
import com.example.digitalwalletwalletservice.entity.TransactionEntity;

public interface TransactionMapper {

    static TransactionResponseDto toResponseDto(TransactionEntity transactionEntity) {
        return TransactionResponseDto.builder()
                .id(transactionEntity.getId())
                .walletId(transactionEntity.getWalletId())
                .transferId(transactionEntity.getTransferId())
                .type(transactionEntity.getType())
                .amount(transactionEntity.getAmount())
                .balanceBefore(transactionEntity.getBalanceBefore())
                .balanceAfter(transactionEntity.getBalanceAfter())
                .createdAt(transactionEntity.getCreatedAt())
                .build();
    }
}
