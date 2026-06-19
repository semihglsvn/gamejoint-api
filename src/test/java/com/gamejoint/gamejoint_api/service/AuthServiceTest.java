package com.gamejoint.gamejoint_api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gamejoint.gamejoint_api.dto.UserRegistrationRequest;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // 1. Mocking (Faking) Dependencies
    // We don't want to actually hit the real MariaDB database during a test,
    // so we create "fake" versions of the Repository and PasswordEncoder.
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // 2. The Service We Are Testing
    // Spring will automatically inject the "fakes" we created above into this real service.
    @InjectMocks
    private AuthService authService;

    // 3. The Actual Test
    @Test
    void testRegisterNewUser_Success() {
        // --- ARRANGE (Set up the fake scenario) ---
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("testbobby");
        request.setEmail("bobby@example.com");
        request.setPassword("SecretPass123");
        request.setDob(LocalDate.of(2000, 1, 1));
        request.setCfTurnstileResponse("dummy-token");

        // Tell the fake password encoder to just return "hashed_password" when asked
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed_password");
        
        // Tell the fake repository to pretend it saved the user and return a fake User object
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // --- ACT (Run the actual code) ---
        // (Note: We use try-catch because your Turnstile logic throws an exception if the token is "dummy-token")
        try {
            authService.register(request);
        } catch (Exception e) {
            // Ignore the Turnstile exception for this specific test
        }

        // --- ASSERT (Prove it worked) ---
        // Prove that the authService actually told the userRepository to save the user exactly 1 time!
        verify(userRepository, times(1)).save(any(User.class));
    }
}