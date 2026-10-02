package com.example.digitalwalletwalletservice.service.impl;

import com.example.digitalwalletwalletservice.dto.TransferRequestDto;
import com.example.digitalwalletwalletservice.dto.TransferResponseDto;
import com.example.digitalwalletwalletservice.entity.TransferEntity;
import com.example.digitalwalletwalletservice.exception.IdempotencyKeyReusedException;
import com.example.digitalwalletwalletservice.mapper.TransferMapper;
import com.example.digitalwalletwalletservice.repository.TransferRepository;
import com.example.digitalwalletwalletservice.service.TransferService;
import com.example.digitalwalletwalletservice.util.TransferRequestHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final TransferExecutor transferExecutor;

    @Override
    public TransferResponseDto transfer(TransferRequestDto requestDto, String idempotencyKey) {
        String requestHash = TransferRequestHasher.hash(requestDto);

        if (StringUtils.hasText(idempotencyKey)) {
            var existingTransfer = transferRepository.findByIdempotencyKey(idempotencyKey);
            if (existingTransfer.isPresent()) {
                return resolveExistingTransfer(existingTransfer.get(), idempotencyKey, requestHash);
            }
        }

        try {
            TransferEntity savedTransfer = transferExecutor.execute(requestDto, idempotencyKey, requestHash);
            log.info("Transfer completed: id={}, source={}, target={}, amount={}",
                    savedTransfer.getId(), requestDto.getSourceWalletId(), requestDto.getTargetWalletId(),
                    requestDto.getAmount());
            return TransferMapper.toResponseDto(savedTransfer);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Idempotency key conflict detected, idempotencyKey={}", idempotencyKey);
            TransferEntity existing = transferRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
            return resolveExistingTransfer(existing, idempotencyKey, requestHash);
        }
    }

    private TransferResponseDto resolveExistingTransfer(TransferEntity existing, String idempotencyKey, String requestHash) {
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new IdempotencyKeyReusedException(idempotencyKey);
        }

        log.info("Duplicate transfer request, idempotencyKey={}, returning existing transfer id={}",
                idempotencyKey, existing.getId());
        return TransferMapper.toResponseDto(existing);
    }
}
