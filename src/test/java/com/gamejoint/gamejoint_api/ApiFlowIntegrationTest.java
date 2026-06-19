package com.gamejoint.gamejoint_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Boots a real embedded Tomcat server on a random, available port
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ApiFlowIntegrationTest {

    // Automatically grabs the random port Tomcat decided to use
    @Value("${local.server.port}")
    private int port;

    // Use the standard HTTP client that we KNOW your compiler already has!
    private final RestTemplate restTemplate = new RestTemplate();

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    public void testCompleteUserJourney() {
        
        // ==========================================
        // ACT 1: REGISTRATION
        // ==========================================
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String registerJson = """
                {
                    "username": "journeyuser",
                    "email": "journey@example.com",
                    "password": "SecurePassword123!",
                    "dob": "2000-01-01",
                    "cfTurnstileResponse": "dummy-token"
                }
                """;

        HttpEntity<String> registerRequest = new HttpEntity<>(registerJson, headers);
        
        // Send POST request using our dynamic base URL
        ResponseEntity<String> regResponse = restTemplate.postForEntity(
                getBaseUrl() + "/api/auth/register", 
                registerRequest, 
                String.class
        );
        
        assertTrue(regResponse.getStatusCode().is2xxSuccessful());

        // ==========================================
        // ACT 2: LOGIN & GET TOKEN
        // ==========================================
        String loginJson = """
                {
                    "usernameOrEmail": "journeyuser",
                    "password": "SecurePassword123!",
                    "cfTurnstileResponse": "dummy-token"
                }
                """;
        
        HttpEntity<String> loginRequest = new HttpEntity<>(loginJson, headers);
        
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                getBaseUrl() + "/api/auth/login", 
                loginRequest, 
                Map.class
        );
        
        assertTrue(loginResponse.getStatusCode().is2xxSuccessful());
        
        String jwtToken = (String) loginResponse.getBody().get("token");
        assertNotNull(jwtToken);

        // ==========================================
        // ACT 3: VIEW SECURE PROFILE
        // ==========================================
        HttpHeaders secureHeaders = new HttpHeaders();
        secureHeaders.setBearerAuth(jwtToken);
        
        HttpEntity<Void> profileRequest = new HttpEntity<>(secureHeaders);
        
        ResponseEntity<Map> profileResponse = restTemplate.exchange(
                getBaseUrl() + "/api/users/profile", 
                HttpMethod.GET, 
                profileRequest, 
                Map.class
        );
        
        assertTrue(profileResponse.getStatusCode().is2xxSuccessful());
        assertEquals("journeyuser", profileResponse.getBody().get("username"));
    }
}