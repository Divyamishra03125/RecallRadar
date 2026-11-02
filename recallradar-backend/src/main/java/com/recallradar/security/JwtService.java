package com.recallradar.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final byte[] secretKeyBytes;
    private final long expirationMs;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${jwt.secret:RecallRadarEnterpriseSecretKeyForProductionTokenSigning2026Antigravity!}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public String generateToken(String username, String role) {
        long now = System.currentTimeMillis();
        long exp = now + expirationMs;

        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", username);
            payload.put("role", role != null ? role.toUpperCase() : "VIEWER");
            payload.put("iat", now / 1000);
            payload.put("exp", exp / 1000);

            String headerBase64 = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String payloadBase64 = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String content = headerBase64 + "." + payloadBase64;

            String signature = sign(content);
            return content + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate JWT token: {}", e.getMessage());
            throw new RuntimeException("Error generating authentication token", e);
        }
    }

    public boolean validateToken(String token) {
        if (token == null || !token.contains(".")) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content);

            // Constant-time signature comparison to prevent timing attacks
            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payloadJson = objectMapper.readTree(payloadBytes);

            long exp = payloadJson.path("exp").asLong(0);
            long nowSec = System.currentTimeMillis() / 1000;
            return exp > nowSec;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payloadJson = objectMapper.readTree(payloadBytes);
            return payloadJson.path("sub").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payloadJson = objectMapper.readTree(payloadBytes);
            return payloadJson.path("role").asText("VIEWER");
        } catch (Exception e) {
            return "VIEWER";
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(hash);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
