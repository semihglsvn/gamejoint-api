package com.gamejoint.gamejoint_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
        
        // 1. Force the correct Content-Type for Cloudflare
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 2. Use MultiValueMap to trigger form-data serialization instead of JSON
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("secret", turnstileSecret);
        map.add("response", token);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        
        try {
            // 3. Send the properly formatted request
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            return false;
        }
    }
}