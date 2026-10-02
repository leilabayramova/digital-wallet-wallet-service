package com.example.digitalwalletwalletservice.service.impl;

import com.example.digitalwalletwalletservice.dto.TransferRequestDto;
import com.example.digitalwalletwalletservice.entity.TransactionEntity;
import com.example.digitalwalletwalletservice.entity.TransferEntity;
import com.example.digitalwalletwalletservice.entity.WalletEntity;
import com.example.digitalwalletwalletservice.enums.TransactionType;
import com.example.digitalwalletwalletservice.enums.TransferStatus;
import com.example.digitalwalletwalletservice.enums.WalletStatus;
import com.example.digitalwalletwalletservice.exception.CurrencyMismatchException;
import com.example.digitalwalletwalletservice.exception.InsufficientBalanceException;
import com.example.digitalwalletwalletservice.exception.InvalidWalletStatusException;
import com.example.digitalwalletwalletservice.exception.SameWalletTransferException;
import com.example.digitalwalletwalletservice.exception.WalletNotFoundException;
import com.example.digitalwalletwalletservice.repository.TransactionRepository;
import com.example.digitalwalletwalletservice.repository.TransferRepository;
import com.example.digitalwalletwalletservice.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferExecutorTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferExecutor transferExecutor;

    @Test
    void execute_shouldMoveMoneyAndCreateTransactions_whenValid() {
        WalletEntity sourceWallet = buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.valueOf(100), "AZN");
        WalletEntity targetWallet = buildWallet(2L, WalletStatus.ACTIVE, BigDecimal.valueOf(50), "AZN");

        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(targetWallet));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRepository.save(any(TransferEntity.class))).thenAnswer(inv -> {
            TransferEntity entity = inv.getArgument(0);
            entity.setId(100L);
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.valueOf(30));

        TransferEntity savedTransfer = transferExecutor.execute(requestDto, null, "hash-1");

        assertThat(savedTransfer.getId()).isEqualTo(100L);
        assertThat(savedTransfer.getSourceWalletId()).isEqualTo(1L);
        assertThat(savedTransfer.getTargetWalletId()).isEqualTo(2L);
        assertThat(savedTransfer.getAmount()).isEqualByComparingTo("30");
        assertThat(savedTransfer.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(savedTransfer.getRequestHash()).isEqualTo("hash-1");

        assertThat(sourceWallet.getBalance()).isEqualByComparingTo("70");
        assertThat(targetWallet.getBalance()).isEqualByComparingTo("80");

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(2)).save(captor.capture());

        List<TransactionEntity> transactions = captor.getAllValues();
        TransactionEntity debit = transactions.get(0);
        assertThat(debit.getWalletId()).isEqualTo(1L);
        assertThat(debit.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(debit.getBalanceBefore()).isEqualByComparingTo("100");
        assertThat(debit.getBalanceAfter()).isEqualByComparingTo("70");

        TransactionEntity credit = transactions.get(1);
        assertThat(credit.getWalletId()).isEqualTo(2L);
        assertThat(credit.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(credit.getBalanceBefore()).isEqualByComparingTo("50");
        assertThat(credit.getBalanceAfter()).isEqualByComparingTo("80");
    }

    @Test
    void execute_shouldPropagateDataIntegrityViolation_withoutSavingTransactions_whenIdempotencyKeyAlreadyTaken() {
        WalletEntity sourceWallet = buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.valueOf(100), "AZN");
        WalletEntity targetWallet = buildWallet(2L, WalletStatus.ACTIVE, BigDecimal.valueOf(50), "AZN");

        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(targetWallet));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRepository.save(any(TransferEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate idempotency key"));

        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.TEN);

        assertThatThrownBy(() -> transferExecutor.execute(requestDto, "race-key", "hash-1"))
                .isInstanceOf(DataIntegrityViolationException.class);

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void execute_shouldThrow_whenSourceAndTargetAreTheSame() {
        TransferRequestDto requestDto = buildRequest(1L, 1L, BigDecimal.TEN);

        assertThatThrownBy(() -> transferExecutor.execute(requestDto, null, "hash-1"))
                .isInstanceOf(SameWalletTransferException.class);

        verifyNoInteractions(walletRepository, transferRepository, transactionRepository);
    }

    @Test
    void execute_shouldThrow_whenSourceWalletNotFound() {
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.ONE);

        assertThatThrownBy(() -> transferExecutor.execute(requestDto, null, "hash-1"))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void execute_shouldThrow_whenTargetWalletNotFound() {
        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.TEN, "AZN")));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.ONE);

        assertThatThrownBy(() -> transferExecutor.execute(requestDto, null, "hash-1"))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void execute_shouldThrow_whenSourceWalletNotActive() {
        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(buildWallet(1L, WalletStatus.BLOCKED, BigDecimal.valueOf(100), "AZN")));
        when(walletRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(buildWallet(2L, WalletStatus.ACTIVE, BigDecimal.TEN, "AZN")));

        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.TEN);

        assertThatThrownBy(() -> transferExecutor.execute(requestDto, null, "hash-1"))
                .isInstanceOf(InvalidWalletStatusException.class);

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrow_whenTargetWalletNotActive() {
        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.valueOf(100), "AZN")));
        when(walletRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(buildWallet(2L, WalletStatus.CLOSED, BigDecimal.TEN, "AZN")));

        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.TEN);

        assertThatThrownBy(() -> transferExecutor.execute(requestDto, null, "hash-1"))
                .isInstanceOf(InvalidWalletStatusException.class);

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrow_whenCurrenciesDoNotMatch() {
        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.valueOf(100), "AZN")));
        when(walletRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(buildWallet(2L, WalletStatus.ACTIVE, BigDecimal.TEN, "USD")));

        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.TEN);

        assertThatThrownBy(() -> transferExecutor.execute(requestDto, null, "hash-1"))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrow_whenBalanceIsInsufficient() {
        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.valueOf(10), "AZN")));
        when(walletRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(buildWallet(2L, WalletStatus.ACTIVE, BigDecimal.TEN, "AZN")));

        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.valueOf(30));

        assertThatThrownBy(() -> transferExecutor.execute(requestDto, null, "hash-1"))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletRepository, never()).save(any());
    }

    private WalletEntity buildWallet(Long id, WalletStatus status, BigDecimal balance, String currency) {
        return WalletEntity.builder()
                .id(id)
                .userId(1L)
                .walletNumber("WLT00000000000" + id)
                .balance(balance)
                .currency(currency)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private TransferRequestDto buildRequest(Long sourceId, Long targetId, BigDecimal amount) {
        TransferRequestDto requestDto = new TransferRequestDto();
        requestDto.setSourceWalletId(sourceId);
        requestDto.setTargetWalletId(targetId);
        requestDto.setAmount(amount);
        return requestDto;
    }
}
