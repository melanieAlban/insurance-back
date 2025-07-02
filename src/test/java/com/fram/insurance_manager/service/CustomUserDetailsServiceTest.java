package com.fram.insurance_manager.service;

import com.fram.insurance_manager.entity.User;
import com.fram.insurance_manager.repository.UserRepository;
import com.fram.insurance_manager.service.impl.CustomUserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsServiceImpl userDetailsService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setEmail("test@example.com");
        activeUser.setPassword("encoded-password");
        activeUser.setActive(true);
    }

    @Test
    void shouldReturnUserDetailsWhenUserIsActive() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(activeUser);

        UserDetails result = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(result.getUsername()).isEqualTo("test@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(null);

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("notfound@example.com")
        );

        assertThat(exception.getMessage()).isEqualTo("Credenciales inválidas");
    }

    @Test
    void shouldThrowWhenUserIsInactive() {
        activeUser.setActive(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(activeUser);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userDetailsService.loadUserByUsername("test@example.com")
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Usuario inactivo");
    }
}
