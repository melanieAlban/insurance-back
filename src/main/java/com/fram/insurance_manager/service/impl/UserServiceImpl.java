package com.fram.insurance_manager.service.impl;

import java.util.List;
import java.util.UUID;

import com.fram.insurance_manager.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.repository.UserRepository;
import com.fram.insurance_manager.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(this::userToDto).toList();
    }

    @Override
    public UserDto getById(UUID id) {
        User user = findUserById(id);
        return userToDto(user);
    }

    @Override
    public UserDto update(UUID id, UserDto userDto) {
        User user = findUserById(id);

        modelMapper.map(userDto, user);

        return userToDto(userRepository.save(user));
    }

    @Override
    public void delete(UUID id) {
        User user = findUserById(id);

        userRepository.delete(user);
    }

    private UserDto userToDto(User userDto) {
        return modelMapper.map(userDto, UserDto.class);
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el usuario"));
    }
}
