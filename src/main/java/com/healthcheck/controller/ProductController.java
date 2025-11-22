package com.healthcheck.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.healthcheck.config.MetricsConfig;
import com.healthcheck.dto.ProductCreateRequest;
import com.healthcheck.dto.ProductResponse;
import com.healthcheck.entity.User;
import com.healthcheck.service.ProductService;
import com.healthcheck.service.UserService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1")
public class ProductController {
    
    private final ProductService productService;
    private final UserService userService;
    private final MetricsConfig metricsConfig;
    
    @Autowired
    public ProductController(ProductService productService, UserService userService, MetricsConfig metricsConfig) {
        this.productService = productService;
        this.userService = userService;
        this.metricsConfig = metricsConfig;
    }
    
    @PostMapping("/product")
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductCreateRequest request, 
                                          Authentication authentication) {
        log.info("POST /v1/product - Creating product with SKU: {} by user: {}", 
                 request.getSku(), authentication.getName());
        
        Counter counter = metricsConfig.getApiCounter("POST_v1_product");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            String email = authentication.getName();
            
            Timer.Sample dbSample = Timer.start();
            User user = userService.findUserByEmail(email);
            dbSample.stop(metricsConfig.getDatabaseTimer("user_findByEmail"));
            
            if (user == null) {
                log.error("POST /v1/product - User not found: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            if(!user.isVerified()){
                log.warn("POST /v1/product - Email not verified: {}", user.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Email not verified. Please verify your email address before creating products.");
            }
            Timer.Sample dbCreateSample = Timer.start();
            ProductResponse response = productService.createProduct(request, user.getId());
            dbCreateSample.stop(metricsConfig.getDatabaseTimer("product_create"));
            
            log.info("POST /v1/product - Product created successfully with ID: {}, SKU: {}", 
                     response.getId(), response.getSku());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                log.warn("POST /v1/product - Product with SKU {} already exists", request.getSku());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            log.error("POST /v1/product - Failed to create product with SKU: {}", request.getSku(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            sample.stop(metricsConfig.getApiTimer("POST_v1_product"));
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProduct(@PathVariable Long productId) {
        log.info("GET /v1/product/{} - Retrieving product", productId);
        
        Counter counter = metricsConfig.getApiCounter("GET_v1_product");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            Timer.Sample dbSample = Timer.start();
            ProductResponse response = productService.getProductById(productId);
            dbSample.stop(metricsConfig.getDatabaseTimer("product_findById"));
            
            log.info("GET /v1/product/{} - Product retrieved successfully, SKU: {}", 
                     productId, response.getSku());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("GET /v1/product/{} - Product not found", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } finally {
            sample.stop(metricsConfig.getApiTimer("GET_v1_product"));
        }
    }
    
    @PutMapping("/product/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId,
                                        @RequestBody ProductCreateRequest request,
                                        Authentication authentication) {
        log.info("PUT /v1/product/{} - Updating product by user: {}", 
                 productId, authentication.getName());
        
        Counter counter = metricsConfig.getApiCounter("PUT_v1_product");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            String email = authentication.getName();
            
            Timer.Sample dbSample = Timer.start();
            User user = userService.findUserByEmail(email);
            dbSample.stop(metricsConfig.getDatabaseTimer("user_findByEmail"));
            
            if (user == null) {
                log.error("PUT /v1/product/{} - User not found: {}", productId, email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 加入驗證檢查 
            if (!user.isVerified()) {
                log.warn("PUT /v1/product/{} - Email not verified: {}", productId, user.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Email not verified. Please verify your email address before updating products.");
            }
            
            Timer.Sample dbUpdateSample = Timer.start();
            productService.updateProduct(productId, request, user.getId());
            dbUpdateSample.stop(metricsConfig.getDatabaseTimer("product_update"));
            
            log.info("PUT /v1/product/{} - Product updated successfully", productId);
            return ResponseEntity.noContent().build();
            
        } catch (RuntimeException e) {
            String message = e.getMessage().toLowerCase();
            
            if (message.contains("access denied")) {
                log.warn("PUT /v1/product/{} - Access denied for user: {}", 
                         productId, authentication.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (message.contains("not found")) {
                log.warn("PUT /v1/product/{} - Product not found", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            if (message.contains("already exists")) {
                log.warn("PUT /v1/product/{} - SKU already exists: {}", 
                         productId, request.getSku());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            log.error("PUT /v1/product/{} - Failed to update product", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            sample.stop(metricsConfig.getApiTimer("PUT_v1_product"));
        }
    }
    
    @PatchMapping("/product/{productId}")
    public ResponseEntity<?> patchProduct(@PathVariable Long productId,
                                         @RequestBody ProductCreateRequest request,
                                         Authentication authentication) {
        log.info("PATCH /v1/product/{} - Partially updating product by user: {}", 
                 productId, authentication.getName());
        
        Counter counter = metricsConfig.getApiCounter("PATCH_v1_product");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            String email = authentication.getName();
            
            Timer.Sample dbSample = Timer.start();
            User user = userService.findUserByEmail(email);
            dbSample.stop(metricsConfig.getDatabaseTimer("user_findByEmail"));
            
            if (user == null) {
                log.error("PATCH /v1/product/{} - User not found: {}", productId, email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 加入驗證檢查
            if (!user.isVerified()) {
                log.warn("PATCH /v1/product/{} - Email not verified: {}", productId, user.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Email not verified. Please verify your email address before updating products.");
            }
            
            Timer.Sample dbPatchSample = Timer.start();
            productService.patchProduct(productId, request, user.getId());
            dbPatchSample.stop(metricsConfig.getDatabaseTimer("product_patch"));
            
            log.info("PATCH /v1/product/{} - Product patched successfully", productId);
            return ResponseEntity.noContent().build();
            
        } catch (RuntimeException e) {
            String message = e.getMessage().toLowerCase();
            
            if (message.contains("access denied") || message.contains("not found or access denied")) {
                log.warn("PATCH /v1/product/{} - Access denied for user: {}", 
                         productId, authentication.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (message.contains("not found")) {
                log.warn("PATCH /v1/product/{} - Product not found", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            if (message.contains("already exists")) {
                log.warn("PATCH /v1/product/{} - SKU already exists", productId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            log.error("PATCH /v1/product/{} - Failed to patch product", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            sample.stop(metricsConfig.getApiTimer("PATCH_v1_product"));
        }
    }

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId,
                                          Authentication authentication) {
        log.info("DELETE /v1/product/{} - Deleting product by user: {}", 
                 productId, authentication.getName());
        
        Counter counter = metricsConfig.getApiCounter("DELETE_v1_product");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            String email = authentication.getName();
            
            Timer.Sample dbSample = Timer.start();
            User user = userService.findUserByEmail(email);
            dbSample.stop(metricsConfig.getDatabaseTimer("user_findByEmail"));
            
            if (user == null) {
                log.error("DELETE /v1/product/{} - User not found: {}", productId, email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 加入驗證檢查
            if (!user.isVerified()) {
                log.warn("DELETE /v1/product/{} - Email not verified: {}", productId, user.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Email not verified. Please verify your email address before deleting products.");
            }

            Timer.Sample dbDeleteSample = Timer.start();
            productService.deleteProduct(productId, user.getId());
            dbDeleteSample.stop(metricsConfig.getDatabaseTimer("product_delete"));
            
            log.info("DELETE /v1/product/{} - Product deleted successfully", productId);
            return ResponseEntity.noContent().build();
            
        } catch (RuntimeException e) {
            String message = e.getMessage().toLowerCase();
            
            if (message.contains("access denied") || message.contains("not found or access denied")) {
                log.warn("DELETE /v1/product/{} - Access denied for user: {}", 
                         productId, authentication.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (message.contains("not found")) {
                log.warn("DELETE /v1/product/{} - Product not found", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            log.error("DELETE /v1/product/{} - Failed to delete product", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            sample.stop(metricsConfig.getApiTimer("DELETE_v1_product"));
        }
    }
}