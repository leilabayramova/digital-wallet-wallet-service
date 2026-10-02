package com.example.digitalwalletwalletservice.service;

import com.example.digitalwalletwalletservice.dto.CreateWalletRequestDto;
import com.example.digitalwalletwalletservice.dto.DepositRequestDto;
import com.example.digitalwalletwalletservice.dto.WalletResponseDto;

public interface WalletService {

    WalletResponseDto createWallet(CreateWalletRequestDto requestDto);

    WalletResponseDto getWalletById(Long id);

    WalletResponseDto topUp(Long id, DepositRequestDto requestDto);

    WalletResponseDto block(Long id);

    WalletResponseDto activate(Long id);
}
