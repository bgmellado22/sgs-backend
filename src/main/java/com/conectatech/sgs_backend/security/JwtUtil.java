package com.conectatech.sgs_backend.security;

import com.conectatech.sgs_backend.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    // Lectura de clave secreta desde variable de entorno
    @Value("${jwt.secret:}")
    private String secretKey;

    private Key signInKey;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.trim().isEmpty()) {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            this.signInKey = Keys.hmacShaKeyFor(keyBytes);
        } else {
            // Generación de clave efímera segura en memoria para desarrollo local y tests si no hay variable de entorno
            this.signInKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    // Tiempo de expiración
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        String rol = userDetails.getAuthorities().iterator().next().getAuthority();
        extraClaims.put("rol", rol);

        if (userDetails instanceof Usuario) {
            Usuario u = (Usuario) userDetails;
            extraClaims.put("nombreCompleto", u.getNombreCompleto());
            extraClaims.put("nombres", u.getNombres());
            extraClaims.put("apellidoPaterno", u.getApellidoPaterno());
            extraClaims.put("apellidoMaterno", u.getApellidoMaterno());
        }

        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        return this.signInKey;
    }
}