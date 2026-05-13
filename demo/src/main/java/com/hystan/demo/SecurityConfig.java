package com.hystan.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuthService oAuthService;

    public SecurityConfig(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/auth.html")
                .userInfoEndpoint(u -> u.userService(oAuthService))
                .defaultSuccessUrl("/dashboard.html", true)
                .failureUrl("/auth.html?error=true")
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/index.html")
                .permitAll()
            );

        return http.build();
    }
}