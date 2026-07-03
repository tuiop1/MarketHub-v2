package dev.tuiop.accountservice.security.keycloak.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminProperties(
        @NotBlank
        String serverUrl,

        @NotBlank
        String realm,

        @NotBlank
        String clientId,

        @NotBlank
        String clientSecret
) {
}
