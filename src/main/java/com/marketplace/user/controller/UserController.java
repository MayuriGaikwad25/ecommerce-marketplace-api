package com.marketplace.user.controller;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users (Admin)", description = "User listing and vendor promotion")
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<UserResponse> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        return userService.listUsers(pageable);
    }

    @PostMapping("/{id}/promote-to-vendor")
    public UserResponse promoteToVendor(@PathVariable String id) {
        return userService.promoteToVendor(id);
    }
}
