package com.fram.insurance_manager.controller;

import com.fram.insurance_manager.config.auth.AuthenticationRequest;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.service.AuthService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldRegisterUser() {
        // Arrange
        SaveUserDto request = new SaveUserDto();
        request.setEmail("test@example.com");
        request.setPassword("password");

        UserDto expectedResponse = new UserDto();
        expectedResponse.setEmail("test@example.com");

        when(authService.register(any(SaveUserDto.class))).thenReturn(expectedResponse);

        // Act
        UserDto result = authController.registerUser(request);

        // Assert
        verify(authService, times(1)).register(any(SaveUserDto.class));
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldLoginUser() {
        // Arrange
        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password");

        String token = "mocked-jwt-token";

        when(authService.login(any(AuthenticationRequest.class))).thenReturn(token);

        // Act
        String result = authController.login(authRequest);

        // Assert
        verify(authService, times(1)).login(any(AuthenticationRequest.class));
        Assertions.assertThat(result).isEqualTo(token);
    }

    @Test
    void shouldChangePassword() {
        // Arrange
        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("oldPassword");

        String newPassword = "newSecurePassword";
        String expectedMessage = "Password updated successfully";

        when(authService.change(any(AuthenticationRequest.class), eq(newPassword)))
                .thenReturn(expectedMessage);

        // Act
        String result = authController.changePassword(authRequest, newPassword);

        // Assert
        verify(authService, times(1)).change(any(AuthenticationRequest.class), eq(newPassword));
        Assertions.assertThat(result).isEqualTo(expectedMessage);
    }
}
