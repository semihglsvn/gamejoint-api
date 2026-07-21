package com.gamejoint.gamejoint_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginNotificationService {

    private final EmailService emailService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendNewLoginAlert(String email, String ipAddress, String username) {
        // 1. Skip local testing IPs so it doesn't crash the lookup API
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1") || ipAddress.equals("10.0.2.2")) {
            return; 
        }

        // 2. Fetch Geolocation
        String location = "Unknown Location";
        try {
            // ip-api is free for non-commercial use and requires no API key
            String url = "http://ip-api.com/json/" + ipAddress;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && "success".equals(response.get("status"))) {
                String city = (String) response.get("city");
                String country = (String) response.get("country");
                location = city + ", " + country;
            }
        } catch (Exception e) {
            // Silently ignore API failures and fallback to "Unknown Location"
        }

        // 3. Construct HTML Email (Matching your dark mode aesthetic)
        String htmlBody = """
            <div style='background-color: #f4f4f4; padding: 40px 20px; font-family: Arial, sans-serif;'>
                <table align='center' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px; overflow: hidden;'>
                    <tr><td align='center' style='padding: 40px 0; background-color: #222222;'><img src='cid:logo_img' alt='GameJoint Logo' width='200' style='display: block;'></td></tr>
                    <tr><td style='padding: 40px;'>
                        <h2 style='color: #333333; margin-top: 0;'>New Login Detected</h2>
                        <p style='color: #555555;'>Hello <strong>%s</strong>,</p>
                        <p style='color: #555555;'>We noticed a new login to your GameJoint account.</p>
                        
                        <div style='background-color: #f9f9f9; border-left: 4px solid #e74c3c; padding: 15px; margin: 20px 0;'>
                            <p style='margin: 0 0 5px 0; color: #333;'><strong>IP Address:</strong> %s</p>
                            <p style='margin: 0; color: #333;'><strong>Location:</strong> %s</p>
                        </div>
                        
                        <p style='color: #777777; font-size: 12px; line-height: 1.5;'>
                            If this was you, you can safely ignore this email. If you don't recognize this activity, please secure your account by resetting your password immediately.
                        </p>
                    </td></tr>
                </table>
            </div>
            """.formatted(username, ipAddress, location);

        // 4. Send Email
        emailService.sendEmailWithLogo(email, "New Login to GameJoint", htmlBody);
    }
}