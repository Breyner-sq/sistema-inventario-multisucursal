package com.inventario.multisucursal.auth;

import com.inventario.multisucursal.users.RoleCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Emisión y validación de JWT propios, firmados con HS256 (clave simétrica
 * compartida) — no hay servidor OAuth2 externo ni issuer/JWK que gestionar
 * (ADR-005). Claims mínimos, sin datos sensibles: {@code sub} (id de
 * usuario), {@code name}, {@code email}, {@code role}, {@code branchId}
 * (ausente para ADMIN). Nunca se incluye el hash de contraseña ni ningún otro
 * dato sensible.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.expirationMs();
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("name", user.name())
                .claim("email", user.email())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key);

        if (user.branchId() != null) {
            builder.claim("branchId", user.branchId());
        }

        return builder.compact();
    }

    /**
     * @throws JwtException si el token está expirado, mal formado, o su firma
     *                       no corresponde a esta clave (manipulado).
     */
    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String name = claims.get("name", String.class);
        String email = claims.get("email", String.class);
        RoleCode role = RoleCode.valueOf(claims.get("role", String.class));
        Number branchIdClaim = claims.get("branchId", Number.class);
        Long branchId = branchIdClaim != null ? branchIdClaim.longValue() : null;

        return new AuthenticatedUser(userId, name, email, role, branchId);
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
