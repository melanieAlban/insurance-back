package com.fram.insurance_manager.cucumber.steps;

import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.entity.User;
import com.fram.insurance_manager.enums.UserRol;
import com.fram.insurance_manager.repository.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ResponseEntity<List<UserDto>> responseList;

    private static final String SECRET_KEY = "012345678901234567890123456789011881717717171717489849";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10;

    @Given("que estoy autenticado como administrador")
    public void estoy_autenticado_como_administrador() {
        userRepository.deleteAll(); // Limpiar antes de la prueba

        User admin = User.builder()
                .name("Admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("password"))
                .rol(UserRol.ADMIN)
                .active(true)
                .build();
        userRepository.saveAndFlush(admin);
    }

    @And("existen usuarios registrados en el sistema")
    public void existen_usuarios_registrados_en_el_sistema() {
        User user = User.builder()
                .name("Ejemplo")
                .email("ejemplo@example.com")
                .password(passwordEncoder.encode("password"))
                .rol(UserRol.AGENT)
                .active(true)
                .build();
        userRepository.saveAndFlush(user);
    }

    @When("solicito la lista de usuarios")
    public void solicito_la_lista_de_usuarios() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + generarTokenAdmin());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        responseList = restTemplate.exchange(
                "/user",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<UserDto>>() {}
        );
    }

    @Then("debo recibir una lista con al menos 1 usuario")
    public void debo_recibir_una_lista_con_al_menos_un_usuario() {
        assertEquals(200, responseList.getStatusCodeValue());
        List<UserDto> usuarios = responseList.getBody();
        assertNotNull(usuarios, "La lista de usuarios no debe ser nula");
        assertFalse(usuarios.isEmpty(), "La lista de usuarios no debe estar vacía");
    }

    // Generador de JWT para simular autenticación
    private String generarTokenAdmin() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject("admin@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
}
