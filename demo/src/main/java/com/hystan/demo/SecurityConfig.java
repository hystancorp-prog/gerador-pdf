package com.hystan.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/gerar-pdf")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/auth.html",
                                 "/hystan.png", "/hystan_white.png",
                                 "/login", "/oauth2/**").permitAll()
                .requestMatchers("/dashboard.html", "/gerar-pdf").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .defaultSuccessUrl("/dashboard.html", true)
                .failureUrl("/auth.html?error=true")
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/auth.html")
                .permitAll()
            );

        return http.build();
    }
}