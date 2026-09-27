package com.ezsplit.service;

import com.ezsplit.dto.request.RegisterRequest;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserService userService;

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@gmail.com");
        req.setUsername("alice");
        req.setPassword("hello1234");

        when(userRepository.existsByEmail("alice@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email is already registered");
    }
}
