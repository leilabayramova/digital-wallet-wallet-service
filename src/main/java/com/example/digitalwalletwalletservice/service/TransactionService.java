package com.example.digitalwalletwalletservice.service;

import com.example.digitalwalletwalletservice.dto.TransactionResponseDto;

import java.util.List;

public interface TransactionService {

    List<TransactionResponseDto> getTransactionsByWalletId(Long walletId);
}
