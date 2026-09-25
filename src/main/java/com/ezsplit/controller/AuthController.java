package com.ezsplit.controller;

import com.ezsplit.dto.request.LoginRequest;
import com.ezsplit.dto.request.RegisterRequest;
import com.ezsplit.dto.response.AuthResponse;
import com.ezsplit.dto.response.UserResponse;
import com.ezsplit.entity.User;
import com.ezsplit.security.JwtService;
import com.ezsplit.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        User user = userService.authenticate(req);
        String token = jwtService.generateToken(user.getUserId(), user.getEmail());

        AuthResponse res = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .user(userService.toResponse(user))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}
