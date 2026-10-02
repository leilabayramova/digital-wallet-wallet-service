package com.example.digitalwalletwalletservice.repository;

import com.example.digitalwalletwalletservice.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
