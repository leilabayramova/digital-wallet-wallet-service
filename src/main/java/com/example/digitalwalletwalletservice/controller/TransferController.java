package com.example.digitalwalletwalletservice.controller;

import com.example.digitalwalletwalletservice.dto.TransferRequestDto;
import com.example.digitalwalletwalletservice.dto.TransferResponseDto;
import com.example.digitalwalletwalletservice.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transfers", description = "Money transfers between wallets")
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "Transfer money between two wallets",
            description = "Supports an optional Idempotency-Key header to safely retry a request without double-transferring")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponseDto transfer(
            @Valid @RequestBody TransferRequestDto requestDto,
            @Parameter(description = "Unique key to make a retried request idempotent")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return transferService.transfer(requestDto, idempotencyKey);
    }
}
