package com.example.digitalwalletwalletservice.controller;

import com.example.digitalwalletwalletservice.dto.CreateWalletRequestDto;
import com.example.digitalwalletwalletservice.dto.DepositRequestDto;
import com.example.digitalwalletwalletservice.dto.TransactionResponseDto;
import com.example.digitalwalletwalletservice.dto.WalletResponseDto;
import com.example.digitalwalletwalletservice.service.TransactionService;
import com.example.digitalwalletwalletservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Wallets", description = "Wallet creation, balance management and status transitions")
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    @Operation(summary = "Create a wallet for a user")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponseDto createWallet(@Valid @RequestBody CreateWalletRequestDto requestDto) {
        return walletService.createWallet(requestDto);
    }

    @Operation(summary = "Get a wallet by id")
    @GetMapping("/{id}")
    public WalletResponseDto getWalletById(@PathVariable Long id) {
        return walletService.getWalletById(id);
    }

    @Operation(summary = "Top up an active wallet")
    @PostMapping("/{id}/top-up")
    public WalletResponseDto topUp(@PathVariable Long id, @Valid @RequestBody DepositRequestDto requestDto) {
        return walletService.topUp(id, requestDto);
    }

    @Operation(summary = "Block an active wallet")
    @PatchMapping("/{id}/block")
    public WalletResponseDto block(@PathVariable Long id) {
        return walletService.block(id);
    }

    @Operation(summary = "Activate a blocked wallet")
    @PatchMapping("/{id}/activate")
    public WalletResponseDto activate(@PathVariable Long id) {
        return walletService.activate(id);
    }

    @Operation(summary = "Get the transaction history of a wallet")
    @GetMapping("/{id}/transactions")
    public List<TransactionResponseDto> getTransactions(@PathVariable Long id) {
        return transactionService.getTransactionsByWalletId(id);
    }
}
