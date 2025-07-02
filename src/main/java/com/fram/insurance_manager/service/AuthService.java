package com.fram.insurance_manager.service;

import com.fram.insurance_manager.config.auth.AuthenticationRequest;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.dto.UserDto;

public interface AuthService {
    UserDto register(SaveUserDto request);

    String login(AuthenticationRequest authenticationRequest);

    String change(AuthenticationRequest authenticationRequest, String newPassword);
}
