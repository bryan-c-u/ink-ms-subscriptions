package com.inklusport.subscriptions.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Consultas internas a ink-ms-users para resolver UUID ↔ email.
 */
@Component
@Slf4j
public class UsersServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${users.service.url:http://localhost:3002}")
    private String usersServiceUrl;

    public String findIdByEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(usersServiceUrl + "/api/internal/users/id-by-email")
                    .queryParam("email", email)
                    .toUriString();
            Map<String, String> body = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, String>>() {
                    }).getBody();
            if (body == null) {
                return null;
            }
            String id = body.get("id");
            return (id == null || id.isBlank()) ? null : id.trim();
        } catch (Exception e) {
            log.debug("No se resolvió UUID para {}: {}", email, e.getMessage());
            return null;
        }
    }

    public String findEmailById(String userId) {
        if (userId == null || userId.isBlank() || userId.contains("@")) {
            return userId != null && userId.contains("@") ? userId : null;
        }
        try {
            Map<?, ?> body = restTemplate.getForObject(
                    usersServiceUrl + "/api/internal/users/{id}", Map.class, userId);
            if (body == null) {
                return null;
            }
            Object email = body.get("email");
            if (email == null) {
                return null;
            }
            String value = String.valueOf(email).trim();
            return value.contains("@") ? value : null;
        } catch (Exception e) {
            log.debug("No se resolvió email para {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
