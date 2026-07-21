package com.marketplace.user.dto;

public record LoginResponse(String token, String tokenType, UserResponse user) {}
