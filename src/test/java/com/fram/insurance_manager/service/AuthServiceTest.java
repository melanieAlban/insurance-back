package com.fram.insurance_manager.service;

import com.fram.insurance_manager.config.auth.AuthenticationRequest;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.entity.User;
import com.fram.insurance_manager.repository.UserRepository;
import com.fram.insurance_manager.service.impl.AuthServiceImpl;
import com.fram.insurance_manager.service.impl.CustomUserDetailsServiceImpl;
import com.fram.insurance_manager.util.JwtUtil;
import com.fram.insurance_manager.util.UserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsServiceImpl userDetailsService;
    @Mock private JwtUtil jwtUtil;
    @Mock private ModelMapper modelMapper;
    @Mock private UserUtil userUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private SaveUserDto saveUserDto;
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        saveUserDto = new SaveUserDto();
        saveUserDto.setEmail("test@example.com");

        user = new User();
        user.setEmail("test@example.com");

        userDto = new UserDto();
        userDto.setEmail("test@example.com");
    }

    @Test
    void shouldRegisterNewUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(null);
        when(userUtil.generateRandomPassword()).thenReturn("random-password");
        when(passwordEncoder.encode("random-password")).thenReturn("encoded-password");

        saveUserDto.setPassword("encoded-password"); // Updated after encoding
        when(modelMapper.map(saveUserDto, User.class)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        UserDto result = authService.register(saveUserDto);

        verify(userUtil).generateRandomPassword();
        verify(userUtil).sendCredentialsToUser("test@example.com", "test@example.com", "random-password");
        verify(userRepository).save(user);

        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldThrowIfUserAlreadyExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(user);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(saveUserDto)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(exception.getReason()).contains("Ya existe un usuario registrado con el correo");
    }

    @Test
    void shouldLoginSuccessfully() {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail("test@example.com");
        request.setPassword("1234");

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");

        String token = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void shouldPropagateExceptionOnLoginFailure() {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        RuntimeException authException = new RuntimeException("Authentication failed");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(authException);

        Exception exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertThat(exception).isSameAs(authException);
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail("test@example.com");
        request.setPassword("old-password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(user);
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded-password");

        String result = authService.change(request, "new-password");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(user);
        verify(userUtil).sendCredentialsToUser("test@example.com", "test@example.com", "new-password");

        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        assertThat(result).isEqualTo("Password successfully changed.");
    }

    @Test
    void shouldPropagateExceptionOnChangePasswordFailure() {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        RuntimeException authException = new RuntimeException("Authentication failed");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(authException);

        Exception exception = assertThrows(
                RuntimeException.class,
                () -> authService.change(request, "new-password")
        );

        assertThat(exception).isSameAs(authException);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
