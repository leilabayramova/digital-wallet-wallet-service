package com.example.digitalwalletwalletservice.repository;

import com.example.digitalwalletwalletservice.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    boolean existsByWalletNumber(String walletNumber);
}