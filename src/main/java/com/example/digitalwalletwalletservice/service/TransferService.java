package com.example.digitalwalletwalletservice.service;

import com.example.digitalwalletwalletservice.dto.TransferRequestDto;
import com.example.digitalwalletwalletservice.dto.TransferResponseDto;

public interface TransferService {

    TransferResponseDto transfer(TransferRequestDto requestDto, String idempotencyKey);
}
