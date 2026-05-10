package com.example.minitasks.auth;

import com.example.minitasks.auth.dto.LoginRequest;
import com.example.minitasks.auth.dto.RefreshRequest;
import com.example.minitasks.auth.dto.RegisterRequest;
import com.example.minitasks.auth.dto.TokenResponse;
import com.example.minitasks.common.Email;
import com.example.minitasks.common.PasswordPolicy;
import com.example.minitasks.common.exceptions.NotFoundException;
import com.example.minitasks.common.exceptions.ValidationException;
import com.example.minitasks.user.User;
import com.example.minitasks.user.UserRepository;
import com.example.minitasks.user.UserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users,
                       UserService userService,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.users = users;
        this.userService = userService;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenResponse register(RegisterRequest req) {
        Email email = Email.of(req.email());
        PasswordPolicy.validate(req.password());
        User user = userService.register(email.value(), req.password());
        return issueTokens(user);
    }

    public TokenResponse login(LoginRequest req) {
        Email email = Email.of(req.email());
        User user = users.findByEmail(email.value())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is not active");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return issueTokens(user);
    }

    public TokenResponse refresh(RefreshRequest req) {
        UUID userId;
        try {
            userId = jwtService.userIdFromRefresh(req.refreshToken());
        } catch (Exception e) {
            throw new ValidationException("REFRESH_INVALID", "Refresh token is invalid");
        }
        String hash = sha256(req.refreshToken());
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new ValidationException("REFRESH_INVALID", "Refresh token is not recognized"));
        if (stored.isExpired()) {
            refreshTokens.delete(stored);
            throw new ValidationException("REFRESH_EXPIRED", "Refresh token expired");
        }
        // single-use: delete then issue a new pair
        refreshTokens.delete(stored);
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        return issueTokens(user);
    }

    public void logout(UUID userId) {
        refreshTokens.deleteAllByUserId(userId);
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtService.issueAccess(user);
        String refresh = jwtService.issueRefresh(user);
        OffsetDateTime exp = jwtService.refreshExpiry(refresh).atOffset(ZoneOffset.UTC);
        refreshTokens.save(new RefreshToken(UUID.randomUUID(), user.getId(), sha256(refresh), exp));
        return TokenResponse.bearer(access, refresh);
    }

    static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
