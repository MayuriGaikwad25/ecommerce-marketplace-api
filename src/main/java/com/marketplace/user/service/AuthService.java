package com.marketplace.user.service;

import com.marketplace.user.dto.LoginRequest;
import com.marketplace.user.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
