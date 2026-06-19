package com.gamejoint.gamejoint_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // We disable CSRF because JWTs are immune to traditional CSRF attacks
            .csrf(csrf -> csrf.disable())
            
         // Define exactly who is allowed to visit which URLs
            .authorizeHttpRequests(auth -> auth
                
                // THE PUBLIC LOBBY
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/games/**").permitAll()
                .requestMatchers("/error").permitAll() // <--- ADD THIS EXACT LINE
                
                // THE SECURED VAULT (Requires the Bearer Token)
                .requestMatchers("/api/reviews/**").authenticated()
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/reports/**").authenticated()
                
                // Lock down absolutely everything else by default
                .anyRequest().authenticated()
            )
            // Enforce the "Amnesia" policy. Tell Spring NOT to use server-side sessions.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Put our custom Bouncer AT THE VERY FRONT of the line
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}