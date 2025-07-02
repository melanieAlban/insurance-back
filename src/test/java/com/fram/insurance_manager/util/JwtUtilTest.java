package com.fram.insurance_manager.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {
    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtUtil jwtUtil;

    @Mock
    private UserDetails otherUser;

    private UserDetails adminUser;
    private final String adminMail = "admin@example.com";

    private String forgeToken(String username,
                              Date issuedAt,
                              Date expiration,
                              Map<String, Object> claims) throws Exception {
        Field secretField = JwtUtil.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        String secret = (String) secretField.get(jwtUtil);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    @BeforeEach
    void setup() {
        adminUser = new User(
                adminMail,
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void generatedTokenContainsUsernameRoleAndIsFresh() {
        String token = jwtUtil.generateToken(adminUser);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(adminMail);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    void validTokenMatchingUser_returnsTrue() {
        String token = jwtUtil.generateToken(adminUser);
        assertThat(jwtUtil.validateToken(token, adminUser)).isTrue();
    }

    @Test
    void validTokenUsernameMismatch_returnsFalse() {
        String token = jwtUtil.generateToken(adminUser);
        when(otherUser.getUsername()).thenReturn("other@mail");
        assertThat(jwtUtil.validateToken(token, otherUser)).isFalse();
    }

    @Test
    void expiredTokenMatchingUser_throwsExpiredJwtException() throws Exception {
        Date past = new Date(System.currentTimeMillis() - 3_600_000);
        String token = forgeToken(adminMail, past, past, Map.of("role", "ADMIN"));
        assertThatThrownBy(() -> jwtUtil.validateToken(token, adminUser))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void expiredTokenUsernameMismatch_throwsExpiredJwtException() throws Exception {
        Date past = new Date(System.currentTimeMillis() - 3_600_000);
        String token = forgeToken("someone@mail", past, past, Map.of("role", "CLIENT"));
        assertThatThrownBy(() -> jwtUtil.validateToken(token, otherUser))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void unknownClaim_returnsNull() {
        String token = jwtUtil.generateToken(adminUser);
        String foo = jwtUtil.extractClaim(token, c -> c.get("foo", String.class));
        assertThat(foo).isNull();
    }

    @Test
    void generatedToken_isWellFormedJwt() {
        String token = jwtUtil.generateToken(adminUser);
        assertThat(token.split("\\.")).hasSize(3);
    }
}
