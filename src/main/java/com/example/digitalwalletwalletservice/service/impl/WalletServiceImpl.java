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
import com.example.digitalwalletwalletservice.exception.WalletNotFoundException;
import com.example.digitalwalletwalletservice.mapper.WalletMapper;
import com.example.digitalwalletwalletservice.repository.TransactionRepository;
import com.example.digitalwalletwalletservice.repository.WalletRepository;
import com.example.digitalwalletwalletservice.service.WalletService;
import com.example.digitalwalletwalletservice.util.WalletNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public WalletResponseDto createWallet(CreateWalletRequestDto requestDto) {
        userServiceClient.verifyUserExists(requestDto.getUserId());

        WalletEntity walletEntity = WalletMapper.toEntity(requestDto);
        walletEntity.setWalletNumber(generateUniqueWalletNumber());

        WalletEntity savedWallet = walletRepository.save(walletEntity);
        log.info("Wallet created: id={}, userId={}, walletNumber={}",
                savedWallet.getId(), savedWallet.getUserId(), savedWallet.getWalletNumber());

        return WalletMapper.toResponseDto(savedWallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDto getWalletById(Long id) {
        return WalletMapper.toResponseDto(findWalletOrThrow(id));
    }

    @Override
    @Transactional
    public WalletResponseDto topUp(Long id, DepositRequestDto requestDto) {
        WalletEntity wallet = findWalletOrThrow(id);

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidWalletStatusException(
                    "Cannot top up wallet with id %d because it is %s".formatted(id, wallet.getStatus()));
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(balanceBefore.add(requestDto.getAmount()));
        WalletEntity savedWallet = walletRepository.save(wallet);

        transactionRepository.save(TransactionEntity.builder()
                .walletId(id)
                .type(TransactionType.TOP_UP)
                .amount(requestDto.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(savedWallet.getBalance())
                .build());

        log.info("Wallet topped up: id={}, amount={}, newBalance={}",
                id, requestDto.getAmount(), savedWallet.getBalance());

        return WalletMapper.toResponseDto(savedWallet);
    }

    @Override
    @Transactional
    public WalletResponseDto block(Long id) {
        WalletEntity wallet = findWalletOrThrow(id);

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidWalletStatusException(
                    "Cannot block wallet with id %d because it is %s".formatted(id, wallet.getStatus()));
        }

        wallet.setStatus(WalletStatus.BLOCKED);
        WalletEntity savedWallet = walletRepository.save(wallet);
        log.info("Wallet blocked: id={}", id);

        return WalletMapper.toResponseDto(savedWallet);
    }

    @Override
    @Transactional
    public WalletResponseDto activate(Long id) {
        WalletEntity wallet = findWalletOrThrow(id);

        if (wallet.getStatus() != WalletStatus.BLOCKED) {
            throw new InvalidWalletStatusException(
                    "Cannot activate wallet with id %d because it is %s".formatted(id, wallet.getStatus()));
        }

        wallet.setStatus(WalletStatus.ACTIVE);
        WalletEntity savedWallet = walletRepository.save(wallet);
        log.info("Wallet activated: id={}", id);

        return WalletMapper.toResponseDto(savedWallet);
    }

    private WalletEntity findWalletOrThrow(Long id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException(id));
    }

    private String generateUniqueWalletNumber() {
        String walletNumber;
        do {
            walletNumber = WalletNumberGenerator.generate();
        } while (walletRepository.existsByWalletNumber(walletNumber));

        return walletNumber;
    }
}
