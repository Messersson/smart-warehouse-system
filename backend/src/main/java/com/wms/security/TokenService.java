package com.wms.security;

import com.wms.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class TokenService {

    private static final String TOKEN_PREFIX = "wms1";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String tokenSecret;
    private final long tokenExpirationMinutes;

    public TokenService(
            @Value("${wms.security.token-secret}") String tokenSecret,
            @Value("${wms.security.token-expiration-minutes:720}") long tokenExpirationMinutes
    ) {
        if (!StringUtils.hasText(tokenSecret) || tokenSecret.length() < 32) {
            throw new IllegalStateException("wms.security.token-secret must be at least 32 characters");
        }
        this.tokenSecret = tokenSecret;
        this.tokenExpirationMinutes = tokenExpirationMinutes;
    }

    public String createToken(User user) {
        long expiresAt = Instant.now().plusSeconds(tokenExpirationMinutes * 60).getEpochSecond();
        String payload = String.join("|",
                String.valueOf(user.getId()),
                encode(user.getUsername()),
                encode(user.getRoleCode()),
                String.valueOf(expiresAt)
        );
        String encodedPayload = encode(payload);
        return TOKEN_PREFIX + "." + encodedPayload + "." + sign(encodedPayload);
    }

    public Optional<TokenClaims> parseToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3 || !TOKEN_PREFIX.equals(parts[0])) {
            return Optional.empty();
        }

        String expectedSignature = sign(parts[1]);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        try {
            String[] payload = decode(parts[1]).split("\\|", -1);
            if (payload.length != 4) {
                return Optional.empty();
            }
            long expiresAt = Long.parseLong(payload[3]);
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(
                    Long.parseLong(payload[0]),
                    decode(payload[1]),
                    decode(payload[2]),
                    expiresAt
            ));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return encode(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Token signing failed", exception);
        }
    }

    private String encode(String value) {
        return encode(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
