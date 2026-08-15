package com.gamejoint.gamejoint_api.config;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. LINK CORS CONFIGURATION HERE
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. Disable CSRF (Stateless JWT API)
            .csrf(csrf -> csrf.disable())
            
            // Define access controls
            .authorizeHttpRequests(auth -> auth
                
                // THE PUBLIC LOBBY
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/games/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/game/**").permitAll() 
                .requestMatchers(HttpMethod.GET, "/api/games/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/games/sitemap").permitAll()
                
                // Explicitly allow public profiles and user review feeds
                .requestMatchers(HttpMethod.GET, "/api/users/*/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/user/*").permitAll()
                
                // THE SECURED VAULT
                .requestMatchers("/api/reviews/**").authenticated()
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/reports/**").authenticated()
                
                // Lock down everything else
                .anyRequest().authenticated()
            )
            // Stateless Session Policy
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Custom JWT Filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000", 
            "https://game-joint.net",
            "https://www.game-joint.net"
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allowed request headers
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Mobile-App-Secret", "X-Turnstile-Token"));
        
        // Allow HttpOnly cookies to pass through
        configuration.setAllowCredentials(true); 
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}