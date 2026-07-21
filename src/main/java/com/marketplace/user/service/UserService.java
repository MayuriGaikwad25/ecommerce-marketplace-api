package com.marketplace.user.service;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.user.dto.RegisterRequest;
import com.marketplace.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse register(RegisterRequest request);

    PageResponse<UserResponse> listUsers(Pageable pageable);

    UserResponse promoteToVendor(String userId);
}
