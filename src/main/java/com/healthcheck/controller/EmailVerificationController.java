package com.healthcheck.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthcheck.service.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class EmailVerificationController {
    
    
    private final UserService userService;
    
    @Autowired
    public EmailVerificationController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Verify user's email address using verification link
     * GET /validateEmail?email=someone@example.com&token=uuid-token
     * 
     * @param email User's email address (URL encoded)
     * @param token UUID verification token
     * @return 200 OK if verification successful, 400 Bad Request if failed
     */
    @GetMapping("/validateEmail")
    public ResponseEntity<String> validateEmail(
            @RequestParam(name = "email", required = true) String email,
            @RequestParam(name = "token", required = true) String token) {
        
        log.info("Email verification attempt for email: {}", email);
        
        // Validate input parameters
        if (email == null || email.trim().isEmpty()) {
            log.warn("Email verification failed: email parameter is missing or empty");
            return ResponseEntity.badRequest().body("Error: Email parameter is missing");  // ← 加訊息
        }
        
        if (token == null || token.trim().isEmpty()) {
            log.warn("Email verification failed: token parameter is missing or empty for email: {}", email);
            return ResponseEntity.badRequest().body("Error: Verification token is missing");  // ← 加訊息;
        }
        
        try {
            boolean verificationResult = userService.verifyUserEmail(email.trim(), token.trim());
            
            if (verificationResult) {
                log.info("Email verification successful for user: {}", email);
                return ResponseEntity.ok()
                    .body("✅ Email verified successfully!.");  // ← 成功訊息
            } else {
                log.warn("Email verification failed for user: {} with token: {}", email, token);
                return ResponseEntity.badRequest()
                    .body("❌ Verification failed. The link is invalid or has expired (links expire after 1 minute).");  // ← 失敗訊息
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during email verification for user: {}. Error: {}", 
                        email, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body("Server error during verification. Please try again later.");  // ← 錯誤訊息
        }
    }
}