package com.project.back_end.services;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class TokenService {

    private static final Duration TOKEN_LIFETIME = Duration.ofDays(7);
    private static final String ROLE_CLAIM = "role";

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final String secret;

    public TokenService(AdminRepository adminRepository, DoctorRepository doctorRepository,
                        PatientRepository patientRepository, @Value("${jwt.secret}") String secret) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.secret = secret;
    }

    /** HMAC-SHA key from the configured secret. HS256 needs at least 32 bytes, which jjwt enforces. */
    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issues a token whose subject is the user's email (an admin's username) and which
     * expires after 7 days. The role is stored as a claim so a patient's token can never
     * be accepted on a doctor or admin endpoint, even if the same email exists in both tables.
     */
    public String generateToken(String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim(ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TOKEN_LIFETIME.toMillis()))
                .signWith(getSigningKey())
                .compact();
    }

    /** Verifies the signature and expiry, then returns the subject. Throws JwtException if invalid. */
    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    /** True only if the token is genuine, unexpired, issued for this role, and the user still exists. */
    public boolean validateToken(String token, String role) {
        try {
            Claims claims = parse(token);
            if (!role.equals(claims.get(ROLE_CLAIM, String.class))) {
                return false;
            }
            String subject = claims.getSubject();
            return switch (role) {
                case "admin" -> adminRepository.findByUsername(subject) != null;
                case "doctor" -> doctorRepository.findByEmail(subject) != null;
                case "patient" -> patientRepository.findByEmail(subject) != null;
                default -> false;
            };
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }
}
