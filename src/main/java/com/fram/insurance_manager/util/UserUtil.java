package com.fram.insurance_manager.util;

import com.fram.insurance_manager.config.mail.MailgunService;
import com.fram.insurance_manager.entity.User;
import com.fram.insurance_manager.repository.UserRepository;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserUtil {
    private final UserRepository userRepository;
    private final MailgunService mailgunService;

    public UUID getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Necesita autenticación");
        }

        Object principal = authentication.getPrincipal();
        String userEmail;
        if (principal instanceof UserDetails userDetails) {
            userEmail = userDetails.getUsername();
        } else {
            userEmail = principal.toString();
        }

        User user = userRepository.findByEmail(userEmail);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario");
        }

        return user.getId();
    }

    public String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder generatedPassword = new StringBuilder(10);

        for (int i = 0; i < 10; i++) {
            generatedPassword.append(characters.charAt(random.nextInt(characters.length())));
        }

        return generatedPassword.toString();
    }

    public void sendCredentialsToUser(String email, String username, String rawPassword) {
        String subject = "Acceso a tu cuenta en FRAM Seguros Ecuador";
        String htmlContent = "<div style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>Bienvenido a FRAM Seguros Ecuador</h2>"
                + "<p>Hemos creado tu cuenta en nuestra plataforma. A continuación, encontrarás tus credenciales de acceso:</p>"
                + "<ul>"
                + "<li><strong>Usuario:</strong> " + username + "</li>"
                + "<li><strong>Contraseña:</strong> " + rawPassword + "</li>"
                + "</ul>"
                + "<p>Si tienes alguna duda, no dudes en contactarnos.</p>"
                + "<br>"
                + "<p>Atentamente,<br><strong>FRAM Seguros Ecuador</strong></p>"
                + "</div>";

        try {
            mailgunService.sendHtmlMessage(email, subject, htmlContent);
        } catch (UnirestException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el correo de credenciales");
        }
    }
}
