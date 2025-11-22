package com.healthcheck.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.healthcheck.config.MetricsConfig;
import com.healthcheck.dto.UserCreateRequest;
import com.healthcheck.dto.UserResponse;
import com.healthcheck.dto.UserUpdateRequest;
import com.healthcheck.entity.User;
import com.healthcheck.service.UserService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1")
public class UserController {
    
    private final UserService userService;
    private final MetricsConfig metricsConfig;
    
    @Autowired
    public UserController(UserService userService, MetricsConfig metricsConfig) {
        this.userService = userService;
        this.metricsConfig = metricsConfig;
    }
    
    @PostMapping("/user")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("POST /v1/user - Creating user with email: {}", request.getEmail());  // ← 添加日誌
        
        // CloudWatch Metrics - API 調用計數和響應時間
        Counter counter = metricsConfig.getApiCounter("POST_v1_user");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            // 數據庫操作監控
            Timer.Sample dbSample = Timer.start();
            UserResponse createdUserResponse = userService.createUser(request);
            dbSample.stop(metricsConfig.getDatabaseTimer("user_create"));
            
            log.info("POST /v1/user - User created successfully with ID: {}", createdUserResponse.getId());  // ← 添加日誌
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUserResponse);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                log.warn("POST /v1/user - User already exists: {}", request.getEmail());  // ← 添加警告日誌
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            log.error("POST /v1/user - Failed to create user: {}", request.getEmail(), e);  // ← 添加錯誤日誌（包含 stack trace）
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            sample.stop(metricsConfig.getApiTimer("POST_v1_user"));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId,Authentication authentication) {

        log.info("GET /v1/user/{} - Retrieving user", userId);  // ← 添加日誌

        // CloudWatch Metrics - API 調用計數和響應時間
        Counter counter = metricsConfig.getApiCounter("GET_v1_user");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            // ← 加入認證用戶檢查
            if (authentication == null) {
                log.warn("GET /v1/user/{} - No authentication provided", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // ← 獲取當前登入用戶
            User currentUser = userService.findUserByEmail(authentication.getName());
            if (currentUser == null) {
                log.warn("GET /v1/user/{} - Current user not found", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // ← 檢查是否已驗證
            if (!currentUser.isVerified()) {
                log.warn("GET /v1/user/{} - User email not verified: {}", userId, currentUser.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Email not verified. Please verify your email address before accessing this resource.");
            }
            
            // ← 檢查是否訪問自己的資料
            if (!currentUser.getId().equals(userId)) {
                log.warn("GET /v1/user/{} - Access denied", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 數據庫操作監控
            Timer.Sample dbSample = Timer.start();
            User user = userService.findUserById(userId);
            dbSample.stop(metricsConfig.getDatabaseTimer("user_findById"));
            
            if (user == null) {
                log.warn("GET /v1/user/{} - User not found", userId);  // ← 添加警告日誌
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            UserResponse response = new UserResponse(user);
            log.info("GET /v1/user/{} - User retrieved successfully", userId);  // ← 添加日誌
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("GET /v1/user/{} - Failed to retrieve user", userId, e);  // ← 添加錯誤日誌
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            sample.stop(metricsConfig.getApiTimer("GET_v1_user"));
        }
    }
    
    @PutMapping("/user/{userId}")
    public ResponseEntity<?> updateUserById(@PathVariable Long userId,
                                       @Valid @RequestBody UserUpdateRequest request,
                                       Authentication authentication) {
        
        log.info("PUT /v1/user/{} - Updating user by: {}", userId, authentication.getName());  // ← 添加日誌  

        // CloudWatch Metrics - API 調用計數和響應時間
        Counter counter = metricsConfig.getApiCounter("PUT_v1_user");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            // 檢查是否嘗試更新不允許的字段
            if (request.getUsername() != null) {
                log.warn("PUT /v1/user/{} - Attempted to update username (not allowed)", userId);  // ← 添加日誌
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); //400
            }
            
            // 數據庫操作監控 - 查找當前用戶
            Timer.Sample dbSample = Timer.start();
            User currentUser = userService.findUserByEmail(authentication.getName());
            dbSample.stop(metricsConfig.getDatabaseTimer("user_findByEmail"));
            
            if (currentUser == null) {
                log.error("PUT /v1/user/{} - Current user not found: {}", userId, authentication.getName());  // ← 添加日誌
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); //403
            }

            // ← 加這個檢查！
            if (!currentUser.isVerified()) {
                log.warn("PUT /v1/user/{} - User email not verified: {}", userId, currentUser.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Email not verified. Please verify your email address before updating your profile.");
            }
                
            // Check if user is trying to update their own account
            if (!currentUser.getId().equals(userId)) {
                log.warn("PUT /v1/user/{} - Access denied", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // 數據庫操作監控 - 更新用戶
            Timer.Sample dbUpdateSample = Timer.start();
            userService.updateUser(authentication.getName(), request);
            dbUpdateSample.stop(metricsConfig.getDatabaseTimer("user_update"));
            
            log.info("PUT /v1/user/{} - User updated successfully", userId);  // ← 添加日誌
            return ResponseEntity.noContent().build(); // 返回 204 No Content
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                log.warn("PUT /v1/user/{} - User not found", userId);  // ← 添加日誌
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            log.error("PUT /v1/user/{} - Failed to update user", userId, e);  // ← 添加錯誤日誌
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } finally {
            sample.stop(metricsConfig.getApiTimer("PUT_v1_user"));
        }
    }
}