package com.example.digitalwalletwalletservice.controller;

import com.example.digitalwalletwalletservice.dto.CreateWalletRequestDto;
import com.example.digitalwalletwalletservice.dto.DepositRequestDto;
import com.example.digitalwalletwalletservice.dto.TransactionResponseDto;
import com.example.digitalwalletwalletservice.dto.WalletResponseDto;
import com.example.digitalwalletwalletservice.enums.TransactionType;
import com.example.digitalwalletwalletservice.enums.WalletStatus;
import com.example.digitalwalletwalletservice.exception.InvalidWalletStatusException;
import com.example.digitalwalletwalletservice.exception.WalletNotFoundException;
import com.example.digitalwalletwalletservice.service.TransactionService;
import com.example.digitalwalletwalletservice.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @MockBean
    private TransactionService transactionService;

    @Test
    void createWallet_shouldReturn201_whenRequestValid() throws Exception {
        CreateWalletRequestDto requestDto = new CreateWalletRequestDto();
        requestDto.setUserId(1L);
        requestDto.setCurrency("AZN");

        WalletResponseDto responseDto = buildResponse(1L, WalletStatus.ACTIVE, BigDecimal.ZERO);

        when(walletService.createWallet(any(CreateWalletRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.currency").value("AZN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createWallet_shouldReturn400_whenCurrencyMissing() throws Exception {
        CreateWalletRequestDto requestDto = new CreateWalletRequestDto();
        requestDto.setUserId(1L);

        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.currency").exists());

        verify(walletService, never()).createWallet(any());
    }

    @Test
    void getWalletById_shouldReturn200_whenFound() throws Exception {
        WalletResponseDto responseDto = buildResponse(1L, WalletStatus.ACTIVE, BigDecimal.ZERO);

        when(walletService.getWalletById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/wallets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getWalletById_shouldReturn404_whenNotFound() throws Exception {
        when(walletService.getWalletById(99L)).thenThrow(new WalletNotFoundException(99L));

        mockMvc.perform(get("/api/wallets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found with id: 99"));
    }

    @Test
    void topUp_shouldReturn200() throws Exception {
        DepositRequestDto requestDto = new DepositRequestDto();
        requestDto.setAmount(BigDecimal.TEN);

        WalletResponseDto responseDto = buildResponse(1L, WalletStatus.ACTIVE, BigDecimal.TEN);

        when(walletService.topUp(eq(1L), any(DepositRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/wallets/1/top-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10));
    }

    @Test
    void topUp_shouldReturn409_whenWalletNotActive() throws Exception {
        DepositRequestDto requestDto = new DepositRequestDto();
        requestDto.setAmount(BigDecimal.TEN);

        when(walletService.topUp(eq(1L), any(DepositRequestDto.class)))
                .thenThrow(new InvalidWalletStatusException("Cannot top up wallet with id 1 because it is BLOCKED"));

        mockMvc.perform(post("/api/wallets/1/top-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void block_shouldReturn200() throws Exception {
        WalletResponseDto responseDto = buildResponse(1L, WalletStatus.BLOCKED, BigDecimal.ZERO);

        when(walletService.block(1L)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/wallets/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void activate_shouldReturn200() throws Exception {
        WalletResponseDto responseDto = buildResponse(1L, WalletStatus.ACTIVE, BigDecimal.ZERO);

        when(walletService.activate(1L)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/wallets/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getTransactions_shouldReturn200_whenWalletExists() throws Exception {
        TransactionResponseDto transactionDto = TransactionResponseDto.builder()
                .id(1L)
                .walletId(1L)
                .transferId(10L)
                .type(TransactionType.DEBIT)
                .amount(BigDecimal.TEN)
                .balanceBefore(BigDecimal.valueOf(100))
                .balanceAfter(BigDecimal.valueOf(90))
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionService.getTransactionsByWalletId(1L)).thenReturn(List.of(transactionDto));

        mockMvc.perform(get("/api/wallets/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].walletId").value(1))
                .andExpect(jsonPath("$[0].type").value("DEBIT"));
    }

    @Test
    void getTransactions_shouldReturn404_whenWalletNotFound() throws Exception {
        when(transactionService.getTransactionsByWalletId(99L)).thenThrow(new WalletNotFoundException(99L));

        mockMvc.perform(get("/api/wallets/99/transactions"))
                .andExpect(status().isNotFound());
    }

    private WalletResponseDto buildResponse(Long id, WalletStatus status, BigDecimal balance) {
        return WalletResponseDto.builder()
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
