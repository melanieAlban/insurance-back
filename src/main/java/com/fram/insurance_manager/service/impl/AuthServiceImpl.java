package com.fram.insurance_manager.service.impl;

import com.fram.insurance_manager.config.auth.AuthenticationRequest;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.entity.User;
import com.fram.insurance_manager.repository.UserRepository;
import com.fram.insurance_manager.service.AuthService;
import com.fram.insurance_manager.util.JwtUtil;
import com.fram.insurance_manager.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserUtil userUtil;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Override
    public UserDto register(SaveUserDto request) {
        User existantUser = userRepository.findByEmail(request.getEmail());

        if (existantUser != null) {
            logger.warn("Registro fallido: usuario ya existe con email {}", request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Ya existe un usuario registrado con el correo " + request.getEmail());
        }

        String rawPassword;

        if ("test".equalsIgnoreCase(activeProfile)) {
            rawPassword = request.getPassword(); // usar la que vino en el DTO
        } else {
            rawPassword = userUtil.generateRandomPassword(); // genera aleatoria
            userUtil.sendCredentialsToUser(request.getEmail(), request.getEmail(), rawPassword);
        }

        request.setPassword(passwordEncoder.encode(rawPassword));
        User user = modelMapper.map(request, User.class);

        User savedUser = userRepository.save(user);
        logger.info("Registro exitoso: usuario creado con ID {}", savedUser.getId());

        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public String login(AuthenticationRequest authenticationRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getEmail(),
                            authenticationRequest.getPassword()
                    )
            );
            String token = jwtUtil.generateToken(
                    userDetailsService.loadUserByUsername(authenticationRequest.getEmail())
            );
            logger.info("Inicio de sesión exitoso para {}", authenticationRequest.getEmail());
            return token;
        } catch (Exception e) {
            logger.warn("Inicio de sesión fallido para {}", authenticationRequest.getEmail());
            throw e;
        }
    }

    @Override
    public String change(AuthenticationRequest authenticationRequest, String newPassword) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getEmail(),
                            authenticationRequest.getPassword()
                    )
            );

            User user = userRepository.findByEmail(authenticationRequest.getEmail());
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            userUtil.sendCredentialsToUser(user.getEmail(), user.getEmail(), newPassword);
            logger.info("Cambio de contraseña exitoso para {}", user.getEmail());

            return "Password successfully changed.";
        } catch (Exception e) {
            logger.warn("Cambio de contraseña fallido para {}", authenticationRequest.getEmail());
            throw e;
        }
    }
}
