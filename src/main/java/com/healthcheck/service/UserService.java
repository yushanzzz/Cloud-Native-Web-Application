package com.healthcheck.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthcheck.dto.UserCreateRequest;
import com.healthcheck.dto.UserResponse;
import com.healthcheck.dto.UserUpdateRequest;
import com.healthcheck.entity.User;
import com.healthcheck.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SnsService snsService; // Add SNS service
    
    @Autowired
    public UserService(UserRepository userRepository, SnsService snsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.snsService = snsService;
    }
 
    public UserResponse createUser(UserCreateRequest request) {
        // Check if user with email already exists
        if (userRepository.existsByUsername(request.getEmail())) {
            throw new RuntimeException("User with email " + request.getEmail() + " already exists");
        }
        
        // Create new user entity
        User user = new User();
        user.setUsername(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        
        // Encrypt password using BCrypt
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encryptedPassword);
        
        // Assignment 9: Set verification fields
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationExpiry(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1)); // ← 改這行
        user.setVerified(false); // User is not verified initially
        
        // Save user to database
        User savedUser = userRepository.save(user);
        
        log.info("Created new user with email: {} (unverified)", request.getEmail());
        
        // Assignment 9: Publish SNS message for email verification
        try {
            snsService.publishUserVerificationMessage(
                savedUser.getUsername(), // email
                verificationToken,
                savedUser.getFirstName()
            );
            log.info("Published verification message to SNS for user: {}", savedUser.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish SNS message for user: {}. Error: {}", 
                        savedUser.getUsername(), e.getMessage(), e);
            // Don't fail user creation if SNS fails, just log the error
        }
        
        return new UserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByUsername(email);
        if (user == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        return new UserResponse(user);
    }

    public UserResponse updateUser(String email, UserUpdateRequest request) {
        User user = userRepository.findByUsername(email);
        if (user == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        // Update allowed fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        
        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            String encryptedPassword = passwordEncoder.encode(request.getPassword());
            user.setPassword(encryptedPassword);
        }
        
        User updatedUser = userRepository.save(user);
        return new UserResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public boolean validateUserCredentials(String email, String password) {
        User user = userRepository.findByUsername(email);
        if (user == null) {
            return false;
        }
        
        return passwordEncoder.matches(password, user.getPassword());
    }
    
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByUsername(email);
    }

    @Transactional(readOnly = true)
    public boolean userExistsWithEmail(String email) {
        return userRepository.existsByUsername(email);
    }

    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
    /**
     * Verify user email with token
     * @param email User's email address
     * @param token Verification token
     * @return true if verification successful, false otherwise
     */
    public boolean verifyUserEmail(String email, String token) {
        log.info("Attempting to verify email for user: {}", email);
        
        User user = userRepository.findByUsername(email);
        if (user == null) {
            log.warn("User not found for email verification: {}", email);
            return false;
        }
        
        // 加 debug logs（可選，測試時用）
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);  // ← 使用 UTC
        log.info("Current UTC time: {}, Token expiry: {}", currentTime, user.getVerificationExpiry());
        log.info("Token match: {}, Time valid: {}", 
                 token.equals(user.getVerificationToken()),
                 user.getVerificationExpiry() != null && currentTime.isBefore(user.getVerificationExpiry()));
        
        // Check if token matches and hasn't expired
        if (token.equals(user.getVerificationToken()) && 
            user.getVerificationExpiry() != null && 
            currentTime.isBefore(user.getVerificationExpiry())) {  // ← 使用 UTC
            
            // Mark user as verified and clear verification data
            user.setVerified(true);
            user.setVerificationToken(null);
            user.setVerificationExpiry(null);
            userRepository.save(user);
            
            log.info("Email verification successful for user: {}", user.getUsername());
            return true;
        }
        
        log.warn("Email verification failed - Token expired or invalid for email: {}", email);
        return false;
    }
}