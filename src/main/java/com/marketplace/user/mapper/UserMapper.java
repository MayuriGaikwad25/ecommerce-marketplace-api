package com.marketplace.user.mapper;

import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
