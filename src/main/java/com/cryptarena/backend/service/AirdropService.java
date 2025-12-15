package com.cryptarena.backend.service;

import com.cryptarena.backend.dto.airdrop.AirdropEligibilityDto;
import com.cryptarena.backend.dto.airdrop.AirdropRequestDto;
import com.cryptarena.backend.dto.airdrop.AirdropResponseDto;
import com.cryptarena.backend.entity.Airdrop;
import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.entity.Wallet;
import com.cryptarena.backend.exception.AuthenticationException;
import com.cryptarena.backend.repository.AirdropRepository;
import com.cryptarena.backend.repository.UserRepository;
import com.cryptarena.backend.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirdropService {

    private final AirdropRepository airdropRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${solana.airdrop.private-key}")
    private String airdropPrivateKey;

    @Value("${solana.airdrop.amount:0.05}")
    private BigDecimal airdropAmount;

    @Value("${solana.airdrop.cooldown-hours:8}")
    private int cooldownHours;

    @Value("${solana.rpc.url:https://api.devnet.solana.com}")
    private String rpcUrl;

    @Value("${solana.airdrop.use-faucet-fallback:true}")
    private boolean useFaucetFallback;

    private static final long LAMPORTS_PER_SOL = 1_000_000_000L;

    /**
     * Check if a wallet is eligible for airdrop
     */
    public AirdropEligibilityDto checkEligibility(UUID userId, String walletAddress) {
        // Check if wallet is linked to user
        Wallet wallet = walletRepository.findByAddress(walletAddress)
                .orElseThrow(() -> new AuthenticationException("Wallet not linked to your account"));

        if (!wallet.getUser().getId().equals(userId)) {
            throw new AuthenticationException("Wallet does not belong to your account");
        }

        // Check cooldown
        LocalDateTime cooldownThreshold = LocalDateTime.now().minusHours(cooldownHours);
        boolean hasClaimed = airdropRepository.hasClaimedAfter(walletAddress, cooldownThreshold);

        if (hasClaimed) {
            // Find the last successful claim
            var lastClaim = airdropRepository.findMostRecentSuccessfulAirdrop(walletAddress);
            if (lastClaim.isPresent()) {
                LocalDateTime nextEligible = lastClaim.get().getClaimedAt().plusHours(cooldownHours);
                long secondsUntil = Duration.between(LocalDateTime.now(), nextEligible).getSeconds();
                
                return AirdropEligibilityDto.builder()
                        .eligible(false)
                        .reason("You can claim again in " + formatDuration(secondsUntil))
                        .nextEligibleAt(nextEligible)
                        .secondsUntilEligible(secondsUntil)
                        .build();
            }
        }

        return AirdropEligibilityDto.builder()
                .eligible(true)
                .reason("You are eligible to claim SOL")
                .nextEligibleAt(null)
                .secondsUntilEligible(0L)
                .build();
    }

    /**
     * Process airdrop request
     */
    @Transactional
    public AirdropResponseDto claimAirdrop(UUID userId, AirdropRequestDto request) {
        String walletAddress = request.getWalletAddress();

        // Check eligibility
        AirdropEligibilityDto eligibility = checkEligibility(userId, walletAddress);
        if (!eligibility.isEligible()) {
            throw new IllegalStateException(eligibility.getReason());
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Create airdrop record
        Airdrop airdrop = Airdrop.builder()
                .user(user)
                .walletAddress(walletAddress)
                .amountSol(airdropAmount)
                .status("PROCESSING")
                .claimedAt(LocalDateTime.now())
                .build();

        airdrop = airdropRepository.save(airdrop);

        try {
            String signature;
            
            // Try devnet faucet first if fallback is enabled
            if (useFaucetFallback) {
                try {
                    log.info("Attempting faucet airdrop for {}", walletAddress);
                    signature = requestFaucetAirdrop(walletAddress, airdropAmount);
                    log.info("Faucet airdrop successful");
                } catch (Exception faucetError) {
                    log.warn("Faucet failed ({}), falling back to private key transfer", faucetError.getMessage());
                    signature = transferFromPrivateKey(walletAddress, airdropAmount);
                    log.info("Private key transfer successful");
                }
            } else {
                // Use private key transfer directly
                signature = transferFromPrivateKey(walletAddress, airdropAmount);
            }

            // Update airdrop record
            airdrop.setTransactionSignature(signature);
            airdrop.setStatus("SUCCESS");
            airdrop = airdropRepository.save(airdrop);

            log.info("Airdrop successful: {} SOL to {} (tx: {})", airdropAmount, walletAddress, signature);

            return mapToDto(airdrop);

        } catch (Exception e) {
            log.error("Airdrop failed for wallet {}: {}", walletAddress, e.getMessage(), e);
            
            // Update airdrop record
            airdrop.setStatus("FAILED");
            airdropRepository.save(airdrop);

            throw new RuntimeException("Failed to process airdrop: " + e.getMessage(), e);
        }
    }

    /**
     * Request airdrop using Solana RPC requestAirdrop method (devnet faucet)
     */
    private String requestFaucetAirdrop(String recipientAddress, BigDecimal amountSol) throws Exception {
        // Convert SOL to lamports
        long lamports = amountSol.multiply(BigDecimal.valueOf(LAMPORTS_PER_SOL)).longValue();

        WebClient webClient = webClientBuilder.baseUrl(rpcUrl).build();

        // Request airdrop from devnet faucet
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "requestAirdrop",
                "params", List.of(recipientAddress, lamports)
        );

        log.info("Requesting airdrop: {} lamports ({} SOL) to {}", lamports, amountSol, recipientAddress);

        String response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode jsonNode = objectMapper.readTree(response);
        
        if (jsonNode.has("error")) {
            JsonNode errorNode = jsonNode.get("error");
            String errorMsg = errorNode.get("message").asText();
            int errorCode = errorNode.has("code") ? errorNode.get("code").asInt() : -1;
            
            log.error("Airdrop RPC error (code {}): {}", errorCode, errorMsg);
            
            // Provide user-friendly error messages
            String userMessage = switch (errorMsg.toLowerCase()) {
                case "internal error" -> 
                    "Devnet faucet is currently rate-limited or unavailable. Please try one of these alternatives:\n" +
                    "1. Wait a few minutes and try again\n" +
                    "2. Use the official Solana faucet at https://faucet.solana.com\n" +
                    "3. Your wallet may already have sufficient testnet SOL";
                case "too many requests" ->
                    "Rate limit exceeded. Please wait a few minutes before requesting again.";
                default -> 
                    "Faucet error: " + errorMsg + ". Try the official faucet at https://faucet.solana.com";
            };
            
            throw new RuntimeException(userMessage);
        }

        String signature = jsonNode.get("result").asText();
        log.info("Airdrop signature: {}", signature);

        // Wait a bit for confirmation
        Thread.sleep(2000);

        return signature;
    }

    /**
     * Transfer SOL from private key wallet
     */
    private String transferFromPrivateKey(String recipientAddress, BigDecimal amountSol) throws Exception {
        // Decode private key from Base58
        byte[] privateKeyBytes = SolanaBase58.decode(airdropPrivateKey);
        
        // Extract the 32-byte seed (Solana keypairs are 64 bytes: 32-byte seed + 32-byte public key)
        byte[] seed = Arrays.copyOfRange(privateKeyBytes, 0, 32);
        
        // Derive keypair
        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(seed, 0);
        byte[] publicKeyBytes = privateKey.generatePublicKey().getEncoded();
        
        String fromAddress = SolanaBase58.encode(publicKeyBytes);
        log.info("Transferring from {} to {}", fromAddress, recipientAddress);
        
        // Convert SOL to lamports
        long lamports = amountSol.multiply(BigDecimal.valueOf(LAMPORTS_PER_SOL)).longValue();
        
        WebClient webClient = webClientBuilder.baseUrl(rpcUrl).build();
        
        // 1. Get recent blockhash
        String recentBlockhash = getRecentBlockhash(webClient);
        
        // 2. Build transaction message
        byte[] message = buildTransferMessage(publicKeyBytes, SolanaBase58.decode(recipientAddress), lamports, recentBlockhash);
        
        // 3. Sign message
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        signer.update(message, 0, message.length);
        byte[] signature = signer.generateSignature();
        
        // 4. Build transaction (signatures + message)
        ByteBuffer txBuffer = ByteBuffer.allocate(1 + 64 + message.length);
        txBuffer.put((byte) 1); // 1 signature
        txBuffer.put(signature);
        txBuffer.put(message);
        
        byte[] transaction = txBuffer.array();
        
        // 5. Send transaction
        String txSignature = sendTransaction(webClient, transaction);
        
        log.info("Transfer successful: {} SOL from {} to {} (tx: {})", 
                amountSol, fromAddress, recipientAddress, txSignature);
        
        return txSignature;
    }

    /**
     * Build a Solana transfer message
     */
    private byte[] buildTransferMessage(byte[] fromPubkey, byte[] toPubkey, long lamports, String recentBlockhash) {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        
        // Message header
        buffer.put((byte) 1); // 1 required signature
        buffer.put((byte) 0); // 0 readonly signed accounts
        buffer.put((byte) 1); // 1 readonly unsigned account (system program)
        
        // Account keys (compact-u16 array)
        buffer.put((byte) 3); // 3 accounts
        buffer.put(fromPubkey); // [0] sender (signer, writable)
        buffer.put(toPubkey);   // [1] recipient (writable)
        
        // System program ID
        byte[] systemProgramId = new byte[32]; // 11111111111111111111111111111111
        Arrays.fill(systemProgramId, (byte) 0);
        systemProgramId[0] = 0;
        buffer.put(systemProgramId); // [2] system program
        
        // Recent blockhash
        buffer.put(SolanaBase58.decode(recentBlockhash));
        
        // Instructions (compact-u16 array)
        buffer.put((byte) 1); // 1 instruction
        
        // Instruction: Transfer
        buffer.put((byte) 2); // program id index (system program)
        buffer.put((byte) 2); // 2 accounts in instruction
        buffer.put((byte) 0); // account 0 (from)
        buffer.put((byte) 1); // account 1 (to)
        
        // Instruction data: transfer instruction
        ByteBuffer instructionData = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        instructionData.putInt(2); // Transfer instruction discriminator
        instructionData.putLong(lamports);
        byte[] instructionDataBytes = instructionData.array();
        
        buffer.put((byte) instructionDataBytes.length);
        buffer.put(instructionDataBytes);
        
        // Extract the message
        int messageLength = buffer.position();
        byte[] message = new byte[messageLength];
        buffer.rewind();
        buffer.get(message);
        
        return message;
    }

    /**
     * Get recent blockhash from Solana
     */
    private String getRecentBlockhash(WebClient webClient) throws Exception {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "getLatestBlockhash",
                "params", List.of(Map.of("commitment", "finalized"))
        );

        String response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode jsonNode = objectMapper.readTree(response);
        if (jsonNode.has("error")) {
            throw new RuntimeException("RPC error: " + jsonNode.get("error").get("message").asText());
        }

        return jsonNode.get("result").get("value").get("blockhash").asText();
    }

    /**
     * Send transaction to Solana
     */
    private String sendTransaction(WebClient webClient, byte[] transaction) throws Exception {
        String encodedTx = Base64.getEncoder().encodeToString(transaction);

        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "sendTransaction",
                "params", List.of(
                        encodedTx,
                        Map.of("encoding", "base64", "skipPreflight", false, "preflightCommitment", "finalized")
                )
        );

        String response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode jsonNode = objectMapper.readTree(response);
        if (jsonNode.has("error")) {
            throw new RuntimeException("Transaction failed: " + jsonNode.get("error").get("message").asText());
        }

        return jsonNode.get("result").asText();
    }

    /**
     * Get airdrop history for a user
     */
    public List<AirdropResponseDto> getUserAirdropHistory(UUID userId) {
        return airdropRepository.findByUser_IdOrderByClaimedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Map entity to DTO
     */
    private AirdropResponseDto mapToDto(Airdrop airdrop) {
        return AirdropResponseDto.builder()
                .id(airdrop.getId())
                .walletAddress(airdrop.getWalletAddress())
                .amountSol(airdrop.getAmountSol())
                .transactionSignature(airdrop.getTransactionSignature())
                .status(airdrop.getStatus())
                .claimedAt(airdrop.getClaimedAt())
                .build();
    }

    /**
     * Format duration in human-readable format
     */
    private String formatDuration(long seconds) {
        if (seconds < 0) return "now";
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d hour%s %d minute%s", hours, hours != 1 ? "s" : "", minutes, minutes != 1 ? "s" : "");
        } else if (minutes > 0) {
            return String.format("%d minute%s", minutes, minutes != 1 ? "s" : "");
        } else {
            return String.format("%d second%s", secs, secs != 1 ? "s" : "");
        }
    }

    /**
     * Base58 utility class for Solana address encoding/decoding
     */
    private static class SolanaBase58 {
        private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        private static final BigInteger BASE = BigInteger.valueOf(58);

        public static String encode(byte[] input) {
            if (input.length == 0) {
                return "";
            }

            // Count leading zeros
            int zeroCount = 0;
            while (zeroCount < input.length && input[zeroCount] == 0) {
                zeroCount++;
            }

            // Convert to base58
            BigInteger num = new BigInteger(1, input);
            StringBuilder sb = new StringBuilder();

            while (num.compareTo(BigInteger.ZERO) > 0) {
                BigInteger[] divmod = num.divideAndRemainder(BASE);
                num = divmod[0];
                int digit = divmod[1].intValue();
                sb.insert(0, ALPHABET.charAt(digit));
            }

            // Add leading 1s for leading zeros
            for (int i = 0; i < zeroCount; i++) {
                sb.insert(0, ALPHABET.charAt(0));
            }

            return sb.toString();
        }

        public static byte[] decode(String input) {
            if (input.length() == 0) {
                return new byte[0];
            }

            // Count leading 1s
            int leadingZeros = 0;
            while (leadingZeros < input.length() && input.charAt(leadingZeros) == ALPHABET.charAt(0)) {
                leadingZeros++;
            }

            // Convert from base58
            BigInteger num = BigInteger.ZERO;
            for (int i = leadingZeros; i < input.length(); i++) {
                char c = input.charAt(i);
                int digit = ALPHABET.indexOf(c);
                if (digit < 0) {
                    throw new IllegalArgumentException("Invalid Base58 character: " + c);
                }
                num = num.multiply(BASE).add(BigInteger.valueOf(digit));
            }

            // Convert to bytes
            byte[] bytes = num.toByteArray();

            // Remove sign byte if present
            boolean stripSignByte = bytes.length > 1 && bytes[0] == 0 && bytes[1] < 0;
            int leadingZeroBytes = stripSignByte ? leadingZeros + 1 : leadingZeros;

            // Add leading zero bytes
            byte[] result = new byte[leadingZeroBytes + (stripSignByte ? bytes.length - 1 : bytes.length)];
            System.arraycopy(bytes, stripSignByte ? 1 : 0, result, leadingZeros, bytes.length - (stripSignByte ? 1 : 0));

            return result;
        }
    }
}
