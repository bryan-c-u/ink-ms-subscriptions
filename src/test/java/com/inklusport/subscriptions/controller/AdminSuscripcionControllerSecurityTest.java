package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.config.JwtAuthenticationFilter;
import com.inklusport.subscriptions.config.SecurityConfig;
import com.inklusport.subscriptions.dto.SuscripcionResponse;
import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import com.inklusport.subscriptions.exception.GlobalExceptionHandler;
import com.inklusport.subscriptions.security.JwtTokenProvider;
import com.inklusport.subscriptions.service.SuscripcionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Nota: a diferencia de ink-ms-suscripciones (Boot 3.3.5, Jackson 2), este proyecto corre
 * en Spring Boot 4.1.1 (Jackson 3, {@code tools.jackson.databind.ObjectMapper}). Ya no hace
 * falta un {@code ObjectMapper} de prueba con {@code JavaTimeModule}: el auto-configurado
 * por {@code @WebMvcTest} serializa {@code LocalDateTime} nativamente.
 */
@WebMvcTest(controllers = AdminSuscripcionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "jwt.secret=inklusport2024superSecretKeyForJWTtokenGenerationWith512bitsAlgorithmHS512",
        "jwt.expiration=86400000"
})
class AdminSuscripcionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private SuscripcionService suscripcionService;

    @Test
    void cambiarEstado_conTokenAdmin_devuelve200() throws Exception {
        when(suscripcionService.cambiarEstado(1L, EstadoSuscripcion.SUSPENDIDA)).thenReturn(
                SuscripcionResponse.builder().id(1L).estado(EstadoSuscripcion.SUSPENDIDA).build()
        );

        String token = jwtTokenProvider.generateToken("admin@test.com", List.of("ADMIN"));

        mockMvc.perform(patch("/api/suscripciones/admin/1/estado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUSPENDIDA"));
    }

    @Test
    void cambiarEstado_conTokenOrganizador_devuelve403() throws Exception {
        String token = jwtTokenProvider.generateToken("organizador@test.com", List.of("ORGANIZADOR"));

        mockMvc.perform(patch("/api/suscripciones/admin/1/estado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDA\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acceso denegado"));
    }

    @Test
    void cambiarEstado_sinToken_devuelve401() throws Exception {
        mockMvc.perform(patch("/api/suscripciones/admin/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDA\"}"))
                .andExpect(status().isUnauthorized());
    }
}
