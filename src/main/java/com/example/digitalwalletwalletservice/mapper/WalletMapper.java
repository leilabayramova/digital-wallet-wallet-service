package com.example.digitalwalletwalletservice.mapper;

import com.example.digitalwalletwalletservice.dto.CreateWalletRequestDto;
import com.example.digitalwalletwalletservice.dto.WalletResponseDto;
import com.example.digitalwalletwalletservice.entity.WalletEntity;

public interface WalletMapper {

    static WalletEntity toEntity(CreateWalletRequestDto requestDto) {
        return WalletEntity.builder()
                .userId(requestDto.getUserId())
                .currency(requestDto.getCurrency())
                .build();
    }

    static WalletResponseDto toResponseDto(WalletEntity walletEntity) {
        return WalletResponseDto.builder()
                .id(walletEntity.getId())
                .userId(walletEntity.getUserId())
                .walletNumber(walletEntity.getWalletNumber())
                .balance(walletEntity.getBalance())
                .currency(walletEntity.getCurrency())
                .status(walletEntity.getStatus())
                .createdAt(walletEntity.getCreatedAt())
                .updatedAt(walletEntity.getUpdatedAt())
                .build();
    }
}