package com.fram.insurance_manager.util;

import com.fram.insurance_manager.entity.User;
import com.fram.insurance_manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUtilTest {

    @Mock  private UserRepository userRepository;
    @Mock  private Authentication  authentication;
    @Mock  private UserDetails    userDetails;

    @InjectMocks
    private UserUtil userUtil;

    private User user;
    private final String mail = "test@example.com";

    @BeforeEach
    void init() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(mail);

        SecurityContextHolder.setContext(mock(SecurityContext.class));
    }

    @Test
    void noAuthentication_shouldThrowUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(userUtil::getUserId)
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void userNotFound_shouldThrowNotFound() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(mail);
        when(userRepository.findByEmail(mail)).thenReturn(null);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(authentication);

        assertThatThrownBy(userUtil::getUserId)
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void existingUser_shouldReturnId() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(mail);
        when(userRepository.findByEmail(mail)).thenReturn(user);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(authentication);

        UUID id = userUtil.getUserId();

        assertThat(id).isEqualTo(user.getId());
        verify(userRepository).findByEmail(mail);
    }

    @Test
    void principalNotUserDetails_shouldUseToString() {
        when(authentication.getPrincipal()).thenReturn(mail);
        when(userRepository.findByEmail(mail)).thenReturn(user);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(authentication);

        UUID id = userUtil.getUserId();

        assertThat(id).isEqualTo(user.getId());
        verify(userRepository).findByEmail(mail);
    }
}
