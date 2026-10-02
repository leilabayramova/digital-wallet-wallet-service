package com.example.digitalwalletwalletservice.mapper;

import com.example.digitalwalletwalletservice.dto.TransferResponseDto;
import com.example.digitalwalletwalletservice.entity.TransferEntity;

public interface TransferMapper {

    static TransferResponseDto toResponseDto(TransferEntity transferEntity) {
        return TransferResponseDto.builder()
                .id(transferEntity.getId())
                .sourceWalletId(transferEntity.getSourceWalletId())
                .targetWalletId(transferEntity.getTargetWalletId())
                .amount(transferEntity.getAmount())
                .currency(transferEntity.getCurrency())
                .status(transferEntity.getStatus())
                .createdAt(transferEntity.getCreatedAt())
                .build();
    }
}
