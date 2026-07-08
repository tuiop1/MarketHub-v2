package dev.tuiop.accountservice.security;

import dev.tuiop.accountservice.merchant.MerchantStatus;
import dev.tuiop.accountservice.security.keycloak.KeycloakRealmRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiAccessDeniedHandler apiAccessDeniedHandler
    ) {

        return  http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/customers/register")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/merchants/register")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/customers/")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "api/v1/merchants/me")
                        .hasAnyRole("MERCHANT", "MERCHANT_PENDING")
                        .requestMatchers("/api/v1/customers/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/merchants/me")
                        .hasAnyRole("MERCHANT_PENDING", "MERCHANT", "MERCHANT_REJECTED")
                        .requestMatchers("/api/v1/merchants/**")
                        .permitAll()

                        .requestMatchers("/api/v1/admin/**")

                        .hasRole("ADMIN")

                        .anyRequest()
                        .authenticated()




                )


                .oauth2ResourceServer(oauth2 ->
                        oauth2
                                .authenticationEntryPoint(apiAuthenticationEntryPoint)
                                .accessDeniedHandler(apiAccessDeniedHandler)
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))


                )





                .build();
    }



    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {


        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                new KeycloakRealmRoleConverter()
        );
        converter.setPrincipalClaimName("sub");

        return converter;
    }

}
