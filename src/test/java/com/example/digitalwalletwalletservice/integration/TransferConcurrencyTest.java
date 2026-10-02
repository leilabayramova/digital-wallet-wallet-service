package com.example.digitalwalletwalletservice.integration;

import com.example.digitalwalletwalletservice.dto.TransferRequestDto;
import com.example.digitalwalletwalletservice.entity.WalletEntity;
import com.example.digitalwalletwalletservice.enums.WalletStatus;
import com.example.digitalwalletwalletservice.repository.TransactionRepository;
import com.example.digitalwalletwalletservice.repository.WalletRepository;
import com.example.digitalwalletwalletservice.service.TransferService;
import com.example.digitalwalletwalletservice.util.WalletNumberGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class TransferConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransferService transferService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Long walletAId;
    private Long walletBId;

    @BeforeEach
    void setUp() {
        WalletEntity walletA = walletRepository.save(buildWallet(1001L, BigDecimal.valueOf(10_000)));
        WalletEntity walletB = walletRepository.save(buildWallet(1002L, BigDecimal.valueOf(10_000)));
        walletAId = walletA.getId();
        walletBId = walletB.getId();
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void transfer_shouldKeepBalanceConsistentAndNonNegative_underConcurrentBidirectionalLoad()
            throws InterruptedException, ExecutionException, TimeoutException {
        int transfersEachDirection = 50;
        BigDecimal amount = BigDecimal.TEN;
        int totalTasks = transfersEachDirection * 2;

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < transfersEachDirection; i++) {
            futures.add(executor.submit(() -> runTransfer(startLatch, failures, walletAId, walletBId, amount)));
            futures.add(executor.submit(() -> runTransfer(startLatch, failures, walletBId, walletAId, amount)));
        }

        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();
        boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(terminated).as("all transfers should finish without deadlocking").isTrue();
        assertThat(failures.get()).as("no transfer should fail").isZero();

        WalletEntity walletA = walletRepository.findById(walletAId).orElseThrow();
        WalletEntity walletB = walletRepository.findById(walletBId).orElseThrow();

        assertThat(walletA.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        assertThat(walletB.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        assertThat(walletA.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(walletB.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        assertThat(transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletAId)).hasSize(totalTasks);
        assertThat(transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletBId)).hasSize(totalTasks);
    }

    private void runTransfer(CountDownLatch startLatch, AtomicInteger failures,
                              Long sourceId, Long targetId, BigDecimal amount) {
        try {
            startLatch.await();
            TransferRequestDto requestDto = new TransferRequestDto();
            requestDto.setSourceWalletId(sourceId);
            requestDto.setTargetWalletId(targetId);
            requestDto.setAmount(amount);

            transferService.transfer(requestDto, null);
        } catch (Exception e) {
            failures.incrementAndGet();
        }
    }

    private WalletEntity buildWallet(Long userId, BigDecimal balance) {
        return WalletEntity.builder()
                .userId(userId)
                .walletNumber(WalletNumberGenerator.generate())
                .balance(balance)
                .currency("AZN")
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
