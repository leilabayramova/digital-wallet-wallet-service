package com.example.digitalwalletwalletservice.service.impl;

import com.example.digitalwalletwalletservice.dto.TransactionResponseDto;
import com.example.digitalwalletwalletservice.entity.TransactionEntity;
import com.example.digitalwalletwalletservice.enums.TransactionType;
import com.example.digitalwalletwalletservice.exception.WalletNotFoundException;
import com.example.digitalwalletwalletservice.repository.TransactionRepository;
import com.example.digitalwalletwalletservice.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void getTransactionsByWalletId_shouldReturnTransactions_whenWalletExists() {
        when(walletRepository.existsById(1L)).thenReturn(true);

        TransactionEntity debit = TransactionEntity.builder()
                .id(1L).walletId(1L).transferId(100L).type(TransactionType.DEBIT)
                .amount(BigDecimal.TEN).balanceBefore(BigDecimal.valueOf(100)).balanceAfter(BigDecimal.valueOf(90))
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(debit));

        List<TransactionResponseDto> result = transactionService.getTransactionsByWalletId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(result.get(0).getBalanceAfter()).isEqualByComparingTo("90");
    }

    @Test
    void getTransactionsByWalletId_shouldThrow_whenWalletDoesNotExist() {
        when(walletRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> transactionService.getTransactionsByWalletId(99L))
                .isInstanceOf(WalletNotFoundException.class);
    }
}
