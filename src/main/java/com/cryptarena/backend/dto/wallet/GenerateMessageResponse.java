package com.cryptarena.backend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateMessageResponse {

    private String message;
    private Long timestamp;
    private String nonce;
}

