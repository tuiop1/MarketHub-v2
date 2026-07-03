package dev.tuiop.accountservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher customerRegistration =
                PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/customers/register");
        PathPatternRequestMatcher merchantRegistration =
                PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/merchants/register");

        return http
                .securityMatcher(RequestMatchers.anyOf(customerRegistration, merchantRegistration))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(customerRegistration, merchantRegistration).permitAll()
                )
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) {

        return  http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/customers/**")
                        .hasRole("CUSTOMER")

                        .requestMatchers("/api/v1/merchants/**")
                        .hasAnyRole("MERCHANT_PENDING", "MERCHANT")

                        .requestMatchers("/api/v1/admin/**")

                        .hasRole("ADMIN")

                        .anyRequest()
                        .authenticated()




                )


                .oauth2ResourceServer(oauth2 ->
                        oauth2
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
