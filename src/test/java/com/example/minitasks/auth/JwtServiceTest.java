package com.example.minitasks.auth;

import com.example.minitasks.config.JwtProperties;
import com.example.minitasks.user.Role;
import com.example.minitasks.user.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-32-bytes!!";

    private JwtService service(Duration accessTtl, Duration refreshTtl) {
        return new JwtService(new JwtProperties(SECRET, accessTtl, refreshTtl, "mini-tasks-test"));
    }

    private User user() {
        return new User(UUID.randomUUID(), "alice@example.com", "hash", Role.USER);
    }

    @Test
    void issueAccess_thenParseAccess_returnsClaims() {
        JwtService jwt = service(Duration.ofMinutes(15), Duration.ofDays(30));
        User u = user();

        String token = jwt.issueAccess(u);
        JwtService.ParsedAccess parsed = jwt.parseAccess(token);

        assertThat(parsed.userId()).isEqualTo(u.getId());
        assertThat(parsed.email()).isEqualTo(u.getEmail());
        assertThat(parsed.role()).isEqualTo(Role.USER);
    }

    @Test
    void issueRefresh_thenUserIdFromRefresh_matches() {
        JwtService jwt = service(Duration.ofMinutes(15), Duration.ofDays(30));
        User u = user();

        String refresh = jwt.issueRefresh(u);

        assertThat(jwt.userIdFromRefresh(refresh)).isEqualTo(u.getId());
    }

    @Test
    void parseAccess_rejectsRefreshToken() {
        JwtService jwt = service(Duration.ofMinutes(15), Duration.ofDays(30));
        String refresh = jwt.issueRefresh(user());

        assertThatThrownBy(() -> jwt.parseAccess(refresh))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void expiredToken_throws() throws InterruptedException {
        JwtService jwt = service(Duration.ofMillis(1), Duration.ofMillis(1));
        String token = jwt.issueAccess(user());
        Thread.sleep(50);

        assertThatThrownBy(() -> jwt.parseAccess(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
