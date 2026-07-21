package com.marketplace.user.service.impl;

import com.marketplace.common.security.JwtService;
import com.marketplace.user.dto.LoginRequest;
import com.marketplace.user.dto.LoginResponse;
import com.marketplace.user.entity.User;
import com.marketplace.user.mapper.UserMapper;
import com.marketplace.user.repository.UserRepository;
import com.marketplace.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + request.email()));

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, "Bearer", userMapper.toResponse(user));
    }
}
