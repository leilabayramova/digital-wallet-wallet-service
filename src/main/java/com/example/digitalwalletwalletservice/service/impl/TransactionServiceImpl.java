package com.example.digitalwalletwalletservice.service.impl;

import com.example.digitalwalletwalletservice.dto.TransactionResponseDto;
import com.example.digitalwalletwalletservice.exception.WalletNotFoundException;
import com.example.digitalwalletwalletservice.mapper.TransactionMapper;
import com.example.digitalwalletwalletservice.repository.TransactionRepository;
import com.example.digitalwalletwalletservice.repository.WalletRepository;
import com.example.digitalwalletwalletservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByWalletId(Long walletId) {
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNotFoundException(walletId);
        }

        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }
}
