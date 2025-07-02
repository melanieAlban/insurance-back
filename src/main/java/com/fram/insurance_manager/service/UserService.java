package com.fram.insurance_manager.service;

import java.util.List;
import java.util.UUID;

import com.fram.insurance_manager.dto.UserDto;

public interface UserService {
    List<UserDto> getAll();

    UserDto getById(UUID id);

    UserDto update(UUID id, UserDto userDto);

    void delete(UUID id);
} 