package com.example.digitalwalletwalletservice.service.impl;

import com.example.digitalwalletwalletservice.client.UserServiceClient;
import com.example.digitalwalletwalletservice.dto.CreateWalletRequestDto;
import com.example.digitalwalletwalletservice.dto.DepositRequestDto;
import com.example.digitalwalletwalletservice.dto.WalletResponseDto;
import com.example.digitalwalletwalletservice.entity.TransactionEntity;
import com.example.digitalwalletwalletservice.entity.WalletEntity;
import com.example.digitalwalletwalletservice.enums.TransactionType;
import com.example.digitalwalletwalletservice.enums.WalletStatus;
import com.example.digitalwalletwalletservice.exception.InvalidWalletStatusException;
import com.example.digitalwalletwalletservice.exception.UserNotFoundException;
import com.example.digitalwalletwalletservice.exception.WalletNotFoundException;
import com.example.digitalwalletwalletservice.repository.TransactionRepository;
import com.example.digitalwalletwalletservice.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void createWallet_shouldCreateWallet_whenUserExists() {
        CreateWalletRequestDto requestDto = new CreateWalletRequestDto();
        requestDto.setUserId(1L);
        requestDto.setCurrency("AZN");

        when(walletRepository.existsByWalletNumber(anyString())).thenReturn(false);
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> {
            WalletEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            entity.setBalance(BigDecimal.ZERO);
            entity.setStatus(WalletStatus.ACTIVE);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });

        WalletResponseDto response = walletService.createWallet(requestDto);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCurrency()).isEqualTo("AZN");
        assertThat(response.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        verify(userServiceClient).verifyUserExists(1L);
    }

    @Test
    void createWallet_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        CreateWalletRequestDto requestDto = new CreateWalletRequestDto();
        requestDto.setUserId(99L);
        requestDto.setCurrency("AZN");

        doThrow(new UserNotFoundException(99L)).when(userServiceClient).verifyUserExists(99L);

        assertThatThrownBy(() -> walletService.createWallet(requestDto))
                .isInstanceOf(UserNotFoundException.class);

        verify(walletRepository, never()).save(any());
    }

    @Test
    void getWalletById_shouldReturnWallet_whenExists() {
        WalletEntity entity = buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.TEN);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(entity));

        WalletResponseDto response = walletService.getWalletById(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getWalletById_shouldThrow_whenNotFound() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWalletById(1L))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void topUp_shouldIncreaseBalance_whenWalletActive() {
        WalletEntity entity = buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.valueOf(50));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DepositRequestDto requestDto = new DepositRequestDto();
        requestDto.setAmount(BigDecimal.valueOf(25));

        WalletResponseDto response = walletService.topUp(1L, requestDto);

        assertThat(response.getBalance()).isEqualByComparingTo("75");

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(captor.capture());

        TransactionEntity transaction = captor.getValue();
        assertThat(transaction.getWalletId()).isEqualTo(1L);
        assertThat(transaction.getTransferId()).isNull();
        assertThat(transaction.getType()).isEqualTo(TransactionType.TOP_UP);
        assertThat(transaction.getAmount()).isEqualByComparingTo("25");
        assertThat(transaction.getBalanceBefore()).isEqualByComparingTo("50");
        assertThat(transaction.getBalanceAfter()).isEqualByComparingTo("75");
    }

    @Test
    void topUp_shouldThrow_whenWalletNotActive() {
        WalletEntity entity = buildWallet(1L, WalletStatus.BLOCKED, BigDecimal.valueOf(50));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(entity));

        DepositRequestDto requestDto = new DepositRequestDto();
        requestDto.setAmount(BigDecimal.TEN);

        assertThatThrownBy(() -> walletService.topUp(1L, requestDto))
                .isInstanceOf(InvalidWalletStatusException.class);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void block_shouldBlockActiveWallet() {
        WalletEntity entity = buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.ZERO);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponseDto response = walletService.block(1L);

        assertThat(response.getStatus()).isEqualTo(WalletStatus.BLOCKED);
    }

    @Test
    void block_shouldThrow_whenWalletNotActive() {
        WalletEntity entity = buildWallet(1L, WalletStatus.BLOCKED, BigDecimal.ZERO);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> walletService.block(1L))
                .isInstanceOf(InvalidWalletStatusException.class);
    }

    @Test
    void activate_shouldActivateBlockedWallet() {
        WalletEntity entity = buildWallet(1L, WalletStatus.BLOCKED, BigDecimal.ZERO);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponseDto response = walletService.activate(1L);

        assertThat(response.getStatus()).isEqualTo(WalletStatus.ACTIVE);
    }

    @Test
    void activate_shouldThrow_whenWalletNotBlocked() {
        WalletEntity entity = buildWallet(1L, WalletStatus.ACTIVE, BigDecimal.ZERO);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> walletService.activate(1L))
                .isInstanceOf(InvalidWalletStatusException.class);
    }

    private WalletEntity buildWallet(Long id, WalletStatus status, BigDecimal balance) {
        return WalletEntity.builder()
                .id(id)
                .userId(1L)
                .walletNumber("WLT000000000001")
                .balance(balance)
                .currency("AZN")
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
