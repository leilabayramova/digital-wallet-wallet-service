package com.example.digitalwalletwalletservice.controller;

import com.example.digitalwalletwalletservice.dto.TransferRequestDto;
import com.example.digitalwalletwalletservice.dto.TransferResponseDto;
import com.example.digitalwalletwalletservice.enums.TransferStatus;
import com.example.digitalwalletwalletservice.exception.InsufficientBalanceException;
import com.example.digitalwalletwalletservice.exception.SameWalletTransferException;
import com.example.digitalwalletwalletservice.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    @Test
    void transfer_shouldReturn201_whenRequestValid() throws Exception {
        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.valueOf(30));
        TransferResponseDto responseDto = buildResponse(1L, 2L, BigDecimal.valueOf(30));

        when(transferService.transfer(any(TransferRequestDto.class), isNull())).thenReturn(responseDto);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceWalletId").value(1))
                .andExpect(jsonPath("$.targetWalletId").value(2))
                .andExpect(jsonPath("$.amount").value(30))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void transfer_shouldPassIdempotencyKeyHeaderToService() throws Exception {
        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.TEN);
        TransferResponseDto responseDto = buildResponse(1L, 2L, BigDecimal.TEN);

        when(transferService.transfer(any(TransferRequestDto.class), eq("key-123"))).thenReturn(responseDto);

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        verify(transferService).transfer(any(TransferRequestDto.class), eq("key-123"));
    }

    @Test
    void transfer_shouldReturn400_whenAmountMissing() throws Exception {
        TransferRequestDto requestDto = new TransferRequestDto();
        requestDto.setSourceWalletId(1L);
        requestDto.setTargetWalletId(2L);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.amount").exists());

        verify(transferService, never()).transfer(any(), any());
    }

    @Test
    void transfer_shouldReturn400_whenSameWallet() throws Exception {
        TransferRequestDto requestDto = buildRequest(1L, 1L, BigDecimal.TEN);

        when(transferService.transfer(any(TransferRequestDto.class), isNull()))
                .thenThrow(new SameWalletTransferException(1L));

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_shouldReturn409_whenInsufficientBalance() throws Exception {
        TransferRequestDto requestDto = buildRequest(1L, 2L, BigDecimal.valueOf(1000));

        when(transferService.transfer(any(TransferRequestDto.class), isNull()))
                .thenThrow(new InsufficientBalanceException(1L));

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());
    }

    private TransferRequestDto buildRequest(Long sourceId, Long targetId, BigDecimal amount) {
        TransferRequestDto requestDto = new TransferRequestDto();
        requestDto.setSourceWalletId(sourceId);
        requestDto.setTargetWalletId(targetId);
        requestDto.setAmount(amount);
        return requestDto;
    }

    private TransferResponseDto buildResponse(Long sourceId, Long targetId, BigDecimal amount) {
        return TransferResponseDto.builder()
                .id(1L)
                .sourceWalletId(sourceId)
                .targetWalletId(targetId)
                .amount(amount)
                .currency("AZN")
                .status(TransferStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
