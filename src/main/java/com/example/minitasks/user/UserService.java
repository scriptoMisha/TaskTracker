package com.example.minitasks.user;

import com.example.minitasks.common.exceptions.ConflictException;
import com.example.minitasks.common.exceptions.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String email, String rawPassword) {
        if (users.existsByEmail(email)) {
            throw new ConflictException("USER_EMAIL_TAKEN", "Email already registered");
        }
        User user = new User(UUID.randomUUID(), email, passwordEncoder.encode(rawPassword), Role.USER);
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    public void changeRole(UUID userId, Role newRole) {
        User user = getById(userId);
        user.changeRole(newRole);
    }
}
