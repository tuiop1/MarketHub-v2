package dev.tuiop.gatewayservice.security;

import dev.tuiop.gatewayservice.security.keycloak.KeycloakRealmRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()

                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/auth/customers/register",
                                "/api/v1/auth/merchants/register").permitAll()

                        .pathMatchers(HttpMethod.GET, "/api/v1/customers/me")
                        .hasRole("CUSTOMER")
                        .pathMatchers(HttpMethod.GET, "/api/v1/merchants/me")
                        .hasAnyRole("MERCHANT_PENDING", "MERCHANT", "MERCHANT_REJECTED")
                        .pathMatchers("/api/v1/merchants/**").permitAll()

                        .pathMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/products/purchase/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/merchant/products/**").hasRole("MERCHANT")
                        .pathMatchers(
                                "/api/v1/carts/**",
                                "/api/v1/orders/**",
                                "/api/v1/payments/**").hasRole("CUSTOMER")

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        converter.setPrincipalClaimName("sub");
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
