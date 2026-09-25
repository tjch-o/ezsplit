package com.ezsplit.service;

import com.ezsplit.dto.request.LoginRequest;
import com.ezsplit.dto.request.RegisterRequest;
import com.ezsplit.dto.response.UserResponse;
import com.ezsplit.entity.User;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        String username = req.getUsername().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email is already registered");
        }

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username is already taken");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public User authenticate(LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElseThrow(() ->
            new BusinessException("Invalid email or password")
        );

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Invalid email or password");
        }

        return user;
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
