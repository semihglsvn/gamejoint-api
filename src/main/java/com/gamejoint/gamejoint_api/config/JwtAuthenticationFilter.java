package com.gamejoint.gamejoint_api.config;

import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import com.gamejoint.gamejoint_api.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException; 
import io.jsonwebtoken.JwtException; 
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    // ==========================================
    // EXPLICIT ALLOWLIST (Strict Security)
    // ==========================================
    private static final List<String> PUBLIC_AUTH_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/oauth/login",
            "/api/auth/oauth/complete",
            "/api/auth/verify",
            "/api/auth/verify/resend",
            "/api/auth/password/forgot",
            "/api/auth/password/reset"
    );

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Only bypass the JWT check for explicitly defined public endpoints.
        // This guarantees that routes like /api/auth/me or /api/auth/logout ARE protected.
        return PUBLIC_AUTH_ENDPOINTS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = null;
        final String authHeader = request.getHeader("Authorization");

        // ==========================================
        // 1. EXTRACT JWT (MOBILE OR WEB)
        // ==========================================
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Mobile App: Extract from Header
            jwt = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            // Web App: Extract from HttpOnly Cookie
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        // If no token is found, move along (public endpoints allow it, secured block it later)
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String username;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (ExpiredJwtException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired. Please log in again.");
            return;
        } catch (JwtException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication token.");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            User user = userRepository.findByUsername(username).orElse(null);

            if (user != null && jwtService.isTokenValid(jwt, user)) {
                
                // ==========================================
                // 2. DYNAMIC BAN CHECK (Server Time)
                // ==========================================
                if (Boolean.TRUE.equals(user.getIsBanned())) {
                    if (user.getBanExpiresAt() != null && user.getBanExpiresAt().isBefore(LocalDateTime.now())) {
                        // Ban has expired! Lift it and kill the current "banned" session.
                        user.setIsBanned(false);
                        user.setBanExpiresAt(null);
                        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
                        userRepository.save(user);
                        
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Ban expired. Please log in to refresh your session.");
                        return;
                    } else {
                        // Still banned. Block the request.
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your account is currently suspended.");
                        return;
                    }
                }

                // ==========================================
                // 3. TOKEN REVOCATION (SESSION KILL SWITCH)
                // ==========================================
                Integer jwtTokenVersion = jwtService.extractTokenVersion(jwt);
                if (jwtTokenVersion == null || !jwtTokenVersion.equals(user.getTokenVersion())) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired or revoked. Please log in again.");
                    return;
                }

                // ==========================================
                // 4. HANDOFF TO CONTROLLER 
                // ==========================================
                request.setAttribute("userId", user.getId());

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user, null, new ArrayList<>() 
                );
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}