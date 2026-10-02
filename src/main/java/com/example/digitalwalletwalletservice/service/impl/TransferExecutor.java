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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Runs the money-moving part of a transfer in its own transaction, so that a duplicate
 * idempotency key (caught by the caller) rolls back any wallet balance changes made here
 * instead of letting them commit alongside the already-completed original transfer.
 */
@Component
@RequiredArgsConstructor
public class TransferExecutor {

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransferEntity execute(TransferRequestDto requestDto, String idempotencyKey, String requestHash) {
        Long sourceWalletId = requestDto.getSourceWalletId();
        Long targetWalletId = requestDto.getTargetWalletId();

        if (sourceWalletId.equals(targetWalletId)) {
            throw new SameWalletTransferException(sourceWalletId);
        }

        WalletEntity sourceWallet;
        WalletEntity targetWallet;

        if (sourceWalletId < targetWalletId) {
            sourceWallet = lockWalletOrThrow(sourceWalletId);
            targetWallet = lockWalletOrThrow(targetWalletId);
        } else {
            targetWallet = lockWalletOrThrow(targetWalletId);
            sourceWallet = lockWalletOrThrow(sourceWalletId);
        }

        validateTransfer(sourceWallet, targetWallet, requestDto.getAmount());

        BigDecimal amount = requestDto.getAmount();
        BigDecimal sourceBalanceBefore = sourceWallet.getBalance();
        BigDecimal targetBalanceBefore = targetWallet.getBalance();

        sourceWallet.setBalance(sourceBalanceBefore.subtract(amount));
        targetWallet.setBalance(targetBalanceBefore.add(amount));

        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);

        TransferEntity transfer = TransferEntity.builder()
                .sourceWalletId(sourceWalletId)
                .targetWalletId(targetWalletId)
                .amount(amount)
                .currency(sourceWallet.getCurrency())
                .status(TransferStatus.COMPLETED)
                .idempotencyKey(StringUtils.hasText(idempotencyKey) ? idempotencyKey : null)
                .requestHash(requestHash)
                .build();

        // Intentionally not caught here: if this violates the idempotency_key unique
        // constraint, the exception must propagate so Spring rolls back the wallet
        // balance changes made above instead of committing them twice.
        TransferEntity savedTransfer = transferRepository.save(transfer);

        transactionRepository.save(TransactionEntity.builder()
                .walletId(sourceWalletId)
                .transferId(savedTransfer.getId())
                .type(TransactionType.DEBIT)
                .amount(amount)
                .balanceBefore(sourceBalanceBefore)
                .balanceAfter(sourceWallet.getBalance())
                .build());

        transactionRepository.save(TransactionEntity.builder()
                .walletId(targetWalletId)
                .transferId(savedTransfer.getId())
                .type(TransactionType.CREDIT)
                .amount(amount)
                .balanceBefore(targetBalanceBefore)
                .balanceAfter(targetWallet.getBalance())
                .build());

        return savedTransfer;
    }

    private void validateTransfer(WalletEntity sourceWallet, WalletEntity targetWallet, BigDecimal amount) {
        if (sourceWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidWalletStatusException(
                    "Source wallet %d is %s".formatted(sourceWallet.getId(), sourceWallet.getStatus()));
        }

        if (targetWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidWalletStatusException(
                    "Target wallet %d is %s".formatted(targetWallet.getId(), targetWallet.getStatus()));
        }

        if (!sourceWallet.getCurrency().equals(targetWallet.getCurrency())) {
            throw new CurrencyMismatchException(sourceWallet.getId(), targetWallet.getId());
        }

        if (sourceWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(sourceWallet.getId());
        }
    }

    private WalletEntity lockWalletOrThrow(Long walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }
}
