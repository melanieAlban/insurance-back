package com.fram.insurance_manager.controller;

import com.fram.insurance_manager.config.auth.AuthenticationRequest;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/register")
    public UserDto registerUser(@Valid @RequestBody SaveUserDto request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        return authService.login(authenticationRequest);
    }

    @PostMapping("/change/{newPassword}")
    public String changePassword(@RequestBody AuthenticationRequest authenticationRequest, @PathVariable String newPassword) {
        return authService.change(authenticationRequest, newPassword);
    }
    
}