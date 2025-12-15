package com.cryptarena.backend.dto.airdrop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirdropEligibilityDto {
    
    private boolean eligible;
    private String reason;
    private LocalDateTime nextEligibleAt;
    private Long secondsUntilEligible;
}

