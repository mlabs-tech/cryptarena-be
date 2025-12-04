package com.cryptarena.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

@Service
@Slf4j
public class SolanaSignatureService {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    /**
     * Verify a Solana wallet signature
     * 
     * @param walletAddress The Solana wallet address (base58 encoded public key)
     * @param message The original message that was signed
     * @param signature The signature to verify (base58 or base64 encoded)
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(String walletAddress, String message, String signature) {
        try {
            // Decode the wallet address (base58 public key)
            byte[] publicKeyBytes = decodeBase58(walletAddress);
            
            // Decode the signature (could be base58 or base64)
            byte[] signatureBytes = decodeSignature(signature);
            
            // Get message bytes
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            
            // Create Ed25519 public key parameters
            Ed25519PublicKeyParameters publicKey = new Ed25519PublicKeyParameters(publicKeyBytes, 0);
            
            // Verify the signature
            Ed25519Signer signer = new Ed25519Signer();
            signer.init(false, publicKey);
            signer.update(messageBytes, 0, messageBytes.length);
            
            boolean isValid = signer.verifySignature(signatureBytes);
            log.debug("Signature verification result for wallet {}: {}", walletAddress, isValid);
            
            return isValid;
        } catch (Exception e) {
            log.error("Failed to verify signature for wallet {}: {}", walletAddress, e.getMessage());
            return false;
        }
    }

    /**
     * Decode signature from either base58 or base64 format
     */
    private byte[] decodeSignature(String signature) {
        // Try base64 first (more common from web wallets)
        try {
            return Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException e) {
            // Fall back to base58
            return decodeBase58(signature);
        }
    }

    /**
     * Decode a base58 encoded string to bytes
     * Standard base58 alphabet used by Bitcoin/Solana
     */
    private byte[] decodeBase58(String input) {
        final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        final int BASE = 58;
        
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }
        
        // Count leading zeros
        int leadingZeros = 0;
        for (int i = 0; i < input.length() && input.charAt(i) == '1'; i++) {
            leadingZeros++;
        }
        
        // Convert base58 to big integer
        java.math.BigInteger value = java.math.BigInteger.ZERO;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int digit = ALPHABET.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid base58 character: " + c);
            }
            value = value.multiply(java.math.BigInteger.valueOf(BASE)).add(java.math.BigInteger.valueOf(digit));
        }
        
        // Convert big integer to bytes
        byte[] decoded = value.toByteArray();
        
        // Remove leading zero if present (from BigInteger)
        if (decoded.length > 0 && decoded[0] == 0) {
            byte[] tmp = new byte[decoded.length - 1];
            System.arraycopy(decoded, 1, tmp, 0, tmp.length);
            decoded = tmp;
        }
        
        // Add leading zeros back
        byte[] result = new byte[leadingZeros + decoded.length];
        System.arraycopy(decoded, 0, result, leadingZeros, decoded.length);
        
        return result;
    }

    /**
     * Generate a message to be signed by the wallet
     * Includes timestamp and nonce for replay attack prevention
     */
    public String generateSigningMessage(String walletAddress, String nonce, long timestamp) {
        return String.format(
            "Welcome to CryptArena!\n\n" +
            "Sign this message to link your wallet.\n\n" +
            "Wallet: %s\n" +
            "Nonce: %s\n" +
            "Timestamp: %d\n\n" +
            "This signature does not trigger any blockchain transaction or cost any fees.",
            walletAddress,
            nonce,
            timestamp
        );
    }

    /**
     * Validate that the message timestamp is not too old
     * Prevents replay attacks with old signed messages
     */
    public boolean isMessageTimestampValid(long timestamp, long maxAgeMs) {
        long now = System.currentTimeMillis();
        return (now - timestamp) <= maxAgeMs;
    }
}

