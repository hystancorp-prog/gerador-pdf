package com.hystan.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${FIREBASE_PROJECT_ID}")
    private String firebaseProjectId;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .xssProtection(Customizer.withDefaults())
                .contentTypeOptions(Customizer.withDefaults())
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://www.gstatic.com https://www.googleapis.com; " +
                    "connect-src 'self' https://identitytoolkit.googleapis.com https://securetoken.googleapis.com https://www.googleapis.com; " +
                    "img-src 'self' data: https:; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "frame-ancestors 'none'"
                ))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/webhook/stripe", "/*.html", "/*.png", "/*.ico", "/*.js", "/*.css", "/").permitAll()
                .requestMatchers("/gerar-orcamento", "/gerar-recibo").authenticated()
                .requestMatchers("/empresas", "/empresas/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(firebaseJwtDecoder()))
            );
        return http.build();
    }

    @Bean
    public JwtDecoder firebaseJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withJwkSetUri(jwkSetUri)
            .build();

        String issuer = "https://securetoken.google.com/" + firebaseProjectId;
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(issuer),
            new JwtClaimValidator<Object>("aud", aud -> {
                if (aud instanceof java.util.List) return ((java.util.List<?>) aud).contains(firebaseProjectId);
                return firebaseProjectId.equals(String.valueOf(aud));
            })
        );
        decoder.setJwtValidator(validator);
        return decoder;
    }
}