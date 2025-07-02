package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.entity.User;
import com.fram.insurance_manager.enums.UserRol;
import com.fram.insurance_manager.repository.UserRepository;
import com.fram.insurance_manager.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.modelmapper.convention.MatchingStrategies;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;
    private UUID userId;

    @BeforeEach
    void setUp() {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .rol(UserRol.ADMIN)
                .active(true)
                .build();

        userDto = UserDto.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .rol(UserRol.ADMIN)
                .active(true)
                .build();

    }

    @Test
    void getAll_ShouldReturnListOfUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> result = userService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        UserDto dto = result.get(0);
        assertEquals(userDto.getId(), dto.getId());
        assertEquals(userDto.getName(), dto.getName());
        assertEquals(userDto.getEmail(), dto.getEmail());
        verify(userRepository).findAll();
    }

    @Test
    void getById_WhenUserExists_ShouldReturnUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto result = userService.getById(userId);

        assertEquals(userDto.getId(), result.getId());
        assertEquals(userDto.getName(), result.getName());
        assertEquals(userDto.getEmail(), result.getEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    void getById_WhenUserDoesNotExist_ShouldThrowException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> userService.getById(userId));
        verify(userRepository).findById(userId);
    }

    @Test
    void update_WhenUserExists_ShouldUpdateAndReturnUser() {
        UserDto updatedDto = UserDto.builder()
                .id(userId)
                .name("Updated User")
                .email("updated@example.com")
                .rol(UserRol.ADMIN)
                .active(false)
                .build();

        User updatedEntity = User.builder()
                .id(userId)
                .name("Updated User")
                .email("updated@example.com")
                .rol(UserRol.ADMIN)
                .active(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedEntity);

        UserDto result = userService.update(userId, updatedDto);

        assertEquals(updatedDto.getName(), result.getName());
        assertEquals(updatedDto.getEmail(), result.getEmail());
        assertEquals(updatedDto.getRol(), result.getRol());
        assertEquals(updatedDto.isActive(), result.isActive());

        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void update_WhenUserDoesNotExist_ShouldThrowException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> userService.update(userId, userDto));
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void delete_WhenUserExists_ShouldDeleteUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        userService.delete(userId);

        verify(userRepository).findById(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void delete_WhenUserDoesNotExist_ShouldThrowException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> userService.delete(userId));
        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any());
    }
}
