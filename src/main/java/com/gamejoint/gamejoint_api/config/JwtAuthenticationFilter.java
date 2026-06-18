package com.gamejoint.gamejoint_api.config;

import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import com.gamejoint.gamejoint_api.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Look for the "VIP Pass" in the HTTP Headers
        final String authHeader = request.getHeader("Authorization");

        // If there is no token, let the request pass through. 
        // (Spring Security will block it later if the endpoint is private).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the raw token string
        final String jwt = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);

        // 3. If a username exists and they aren't already authenticated in this cycle
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // Fetch the user to ensure they haven't been deleted since the token was issued
            User user = userRepository.findByUsername(username).orElse(null);

            if (user != null && jwtService.isTokenValid(jwt, user)) {
                
                // ==========================================
                // THE CRITICAL HANDOFF TO THE CONTROLLER
                // ==========================================
                request.setAttribute("userId", user.getId());

                // Tell Spring Security: "This user is fully authenticated and allowed in."
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        new ArrayList<>() // Empty list for roles/authorities
                );
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // Pass the request down the chain to the Controller
        filterChain.doFilter(request, response);
    }
}