package com.example.digitalwalletwalletservice.service.impl;

import com.example.digitalwalletwalletservice.dto.TransferRequestDto;
import com.example.digitalwalletwalletservice.dto.TransferResponseDto;
import com.example.digitalwalletwalletservice.entity.TransferEntity;
import com.example.digitalwalletwalletservice.enums.TransferStatus;
import com.example.digitalwalletwalletservice.exception.IdempotencyKeyReusedException;
import com.example.digitalwalletwalletservice.exception.SameWalletTransferException;
import com.example.digitalwalletwalletservice.repository.TransferRepository;
import com.example.digitalwalletwalletservice.util.TransferRequestHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferExecutor transferExecutor;

    @InjectMocks
    private TransferServiceImpl transferService;

    @Test
    void transfer_shouldDelegateToExecutor_whenNoIdempotencyKey() {
        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.TEN);

        TransferEntity savedTransfer = TransferEntity.builder()
                .id(100L)
                .sourceWalletId(1L)
                .targetWalletId(2L)
                .amount(BigDecimal.TEN)
                .currency("AZN")
                .status(TransferStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(transferExecutor.execute(eq(requestDto), isNull(), anyString())).thenReturn(savedTransfer);

        TransferResponseDto response = transferService.transfer(requestDto, null);

        assertThat(response.getId()).isEqualTo(100L);
        verifyNoInteractions(transferRepository);
    }

    @Test
    void transfer_shouldPropagateBusinessException_fromExecutor() {
        TransferRequestDto requestDto = buildRequest(1L, 1L, BigDecimal.TEN);

        when(transferExecutor.execute(eq(requestDto), isNull(), anyString()))
                .thenThrow(new SameWalletTransferException(1L));

        assertThatThrownBy(() -> transferService.transfer(requestDto, null))
                .isInstanceOf(SameWalletTransferException.class);
    }

    @Test
    void transfer_shouldReturnExistingTransfer_whenIdempotencyKeyAlreadyUsed() {
        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.TEN);

        TransferEntity existingTransfer = TransferEntity.builder()
                .id(5L)
                .sourceWalletId(1L)
                .targetWalletId(2L)
                .amount(BigDecimal.TEN)
                .currency("AZN")
                .status(TransferStatus.COMPLETED)
                .idempotencyKey("existing-key")
                .requestHash(TransferRequestHasher.hash(requestDto))
                .createdAt(LocalDateTime.now())
                .build();

        when(transferRepository.findByIdempotencyKey("existing-key")).thenReturn(Optional.of(existingTransfer));

        TransferResponseDto response = transferService.transfer(requestDto, "existing-key");

        assertThat(response.getId()).isEqualTo(5L);
        verifyNoInteractions(transferExecutor);
    }

    @Test
    void transfer_shouldThrow_whenIdempotencyKeyReusedWithDifferentPayload() {
        TransferRequestDto originalRequest = buildRequest(1L, 2L, BigDecimal.TEN);

        TransferEntity existingTransfer = TransferEntity.builder()
                .id(5L)
                .sourceWalletId(1L)
                .targetWalletId(2L)
                .amount(BigDecimal.TEN)
                .currency("AZN")
                .status(TransferStatus.COMPLETED)
                .idempotencyKey("reused-key")
                .requestHash(TransferRequestHasher.hash(originalRequest))
                .createdAt(LocalDateTime.now())
                .build();

        when(transferRepository.findByIdempotencyKey("reused-key")).thenReturn(Optional.of(existingTransfer));

        TransferRequestDto differentRequest = buildRequest(1L, 2L, BigDecimal.valueOf(999));

        assertThatThrownBy(() -> transferService.transfer(differentRequest, "reused-key"))
                .isInstanceOf(IdempotencyKeyReusedException.class);

        verifyNoInteractions(transferExecutor);
    }

    @Test
    void transfer_shouldRollBackAndReturnExistingTransfer_whenIdempotencyKeyRaceConditionOccurs() {
        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.TEN);

        TransferEntity existingTransfer = TransferEntity.builder()
                .id(5L)
                .sourceWalletId(1L)
                .targetWalletId(2L)
                .amount(BigDecimal.TEN)
                .currency("AZN")
                .status(TransferStatus.COMPLETED)
                .idempotencyKey("race-key")
                .requestHash(TransferRequestHasher.hash(requestDto))
                .createdAt(LocalDateTime.now())
                .build();

        when(transferRepository.findByIdempotencyKey("race-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTransfer));
        when(transferExecutor.execute(eq(requestDto), eq("race-key"), anyString()))
                .thenThrow(new DataIntegrityViolationException("duplicate idempotency key"));

        TransferResponseDto response = transferService.transfer(requestDto, "race-key");

        assertThat(response.getId()).isEqualTo(5L);
    }

    private TransferRequestDto buildRequest(Long sourceId, Long targetId, BigDecimal amount) {
        TransferRequestDto requestDto = new TransferRequestDto();
        requestDto.setSourceWalletId(sourceId);
        requestDto.setTargetWalletId(targetId);
        requestDto.setAmount(amount);
        return requestDto;
    }
}
