package com.cryptarena.backend.dto.airdrop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirdropResponseDto {
    
    private UUID id;
    private String walletAddress;
    private BigDecimal amountSol;
    private String transactionSignature;
    private String status;
    private LocalDateTime claimedAt;
}

