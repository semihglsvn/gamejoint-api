package com.gamejoint.gamejoint_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class TurnstileService {

    @Value("${cloudflare.turnstile.secret}")
    private String turnstileSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
        Map<String, String> request = Map.of(
            "secret", turnstileSecret,
            "response", token
        );
        
        try {
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            return false;
        }
    }
}	