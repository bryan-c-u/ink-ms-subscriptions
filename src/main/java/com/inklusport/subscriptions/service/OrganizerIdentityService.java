package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.client.UsersServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Unifica el identificador de organizador/usuario a UUID de ink-ms-users.
 * Tokens viejos (solo email) se resuelven contra users-ms.
 */
@Service
@RequiredArgsConstructor
public class OrganizerIdentityService {

    private final UsersServiceClient usersServiceClient;

    public String resolveUserId(String principalOrEmail) {
        if (principalOrEmail == null || principalOrEmail.isBlank()) {
            throw new IllegalArgumentException("No se pudo resolver el usuario autenticado");
        }
        String value = principalOrEmail.trim();
        if (isUuidLike(value)) {
            return value;
        }
        if (value.contains("@")) {
            String id = usersServiceClient.findIdByEmail(value);
            if (id != null) {
                return id;
            }
        }
        return value;
    }

    public String resolveEmail(String userIdOrEmail) {
        if (userIdOrEmail == null || userIdOrEmail.isBlank()) {
            return null;
        }
        String value = userIdOrEmail.trim();
        if (value.contains("@")) {
            return value;
        }
        return usersServiceClient.findEmailById(value);
    }

    private static boolean isUuidLike(String value) {
        return value.length() >= 32 && !value.contains("@") && !value.contains(" ");
    }
}
