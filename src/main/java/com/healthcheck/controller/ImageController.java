package com.healthcheck.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.healthcheck.config.MetricsConfig;
import com.healthcheck.entity.Image;
import com.healthcheck.entity.Product;
import com.healthcheck.entity.User;
import com.healthcheck.repository.ImageRepository;
import com.healthcheck.repository.ProductRepository;
import com.healthcheck.repository.UserRepository;
import com.healthcheck.service.S3Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/product/{productId}/image")
@Profile("!test")
public class ImageController {
    
    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MetricsConfig metricsConfig;
    
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png"
    );
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        log.info("POST /v1/product/{}/image - Uploading image: {} by user: {}", 
                 productId, file.getOriginalFilename(), authentication.getName());
        
        Counter counter = metricsConfig.getApiCounter("POST_v1_product_image");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            // Validate file is not empty
            if (file.isEmpty()) {
                log.warn("POST /v1/product/{}/image - Empty file uploaded", productId);
                Map<String, String> error = new HashMap<>();
                error.put("error", "File is empty");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                log.warn("POST /v1/product/{}/image - Invalid file type: {}", productId, contentType);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid file type. Only jpeg, jpg, png are allowed");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Get current user
            String username = authentication.getName();
            Timer.Sample dbSample = Timer.start();
            User user = userRepository.findByUsername(username);
            dbSample.stop(metricsConfig.getDatabaseTimer("user_findByUsername"));
            
            if (user == null) {
                log.error("POST /v1/product/{}/image - User not found: {}", productId, username);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
            }

            // 加入驗證檢查
            if (!user.isVerified()) {
                log.warn("POST /v1/product/{}/image - Email not verified: {}", productId, user.getUsername());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Email not verified. Please verify your email address before uploading images.");
            }

            
            // Verify product exists
            Timer.Sample dbProductSample = Timer.start();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.warn("POST /v1/product/{}/image - Product not found", productId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
                    });
            dbProductSample.stop(metricsConfig.getDatabaseTimer("product_findById"));
            
            // Verify user owns the product
            if (!product.getOwnerUserId().equals(user.getId())) {
                log.warn("POST /v1/product/{}/image - User {} does not own product", 
                         productId, username);
                Map<String, String> error = new HashMap<>();
                error.put("error", "You can only upload images to your own products");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            // Upload to S3
            Timer.Sample s3Sample = Timer.start();
            String s3BucketPath = s3Service.uploadFile(username, file);
            s3Sample.stop(metricsConfig.getS3Timer("image_upload"));
            
            log.debug("POST /v1/product/{}/image - File uploaded to S3: {}", productId, s3BucketPath);
            
            // Save metadata to database
            Image image = new Image();
            image.setProductId(productId);
            image.setUserId(user.getId());
            image.setFileName(file.getOriginalFilename());
            image.setS3BucketPath(s3BucketPath);
            image.setContentType(contentType);
            image.setFileSize(file.getSize());
            
            Timer.Sample dbSaveSample = Timer.start();
            Image savedImage = imageRepository.save(image);
            dbSaveSample.stop(metricsConfig.getDatabaseTimer("image_save"));
            
            // Return image metadata
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("image_id", savedImage.getImageId());
            responseMap.put("product_id", savedImage.getProductId());
            responseMap.put("file_name", savedImage.getFileName());
            responseMap.put("date_created", savedImage.getDateCreated().toString());
            responseMap.put("s3_bucket_path", savedImage.getS3BucketPath());
            
            log.info("POST /v1/product/{}/image - Image uploaded successfully with ID: {}", 
                     productId, savedImage.getImageId());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseMap);
            
        } catch (IOException e) {
            log.error("POST /v1/product/{}/image - Failed to upload image: {}", 
                      productId, file.getOriginalFilename(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload image");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } finally {
            sample.stop(metricsConfig.getApiTimer("POST_v1_product_image"));
        }
    }
    
    @GetMapping("/{imageId}")
    public ResponseEntity<?> getImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        
        log.info("GET /v1/product/{}/image/{} - Retrieving image", productId, imageId);
        
        Counter counter = metricsConfig.getApiCounter("GET_v1_product_image_by_id");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            // Verify image exists
            Timer.Sample dbSample = Timer.start();
            Image image = imageRepository.findById(imageId)
                    .orElseThrow(() -> {
                        log.warn("GET /v1/product/{}/image/{} - Image not found", productId, imageId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
                    });
            dbSample.stop(metricsConfig.getDatabaseTimer("image_findById"));
            
            // Verify image belongs to the product
            if (!image.getProductId().equals(productId)) {
                log.warn("GET /v1/product/{}/image/{} - Image does not belong to product", 
                         productId, imageId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
            }
            
            // Return image metadata
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("image_id", image.getImageId());
            responseMap.put("product_id", image.getProductId());
            responseMap.put("file_name", image.getFileName());
            responseMap.put("date_created", image.getDateCreated().toString());
            responseMap.put("s3_bucket_path", image.getS3BucketPath());
            
            log.info("GET /v1/product/{}/image/{} - Image retrieved successfully", productId, imageId);
            return ResponseEntity.ok(responseMap);
            
        } finally {
            sample.stop(metricsConfig.getApiTimer("GET_v1_product_image_by_id"));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getAllImages(@PathVariable Long productId) {  
        log.info("GET /v1/product/{}/image - Retrieving all images", productId);
        
        Counter counter = metricsConfig.getApiCounter("GET_v1_product_images");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            // Verify product exists
            Timer.Sample dbProductSample = Timer.start();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.warn("GET /v1/product/{}/image - Product not found", productId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
                    });
            dbProductSample.stop(metricsConfig.getDatabaseTimer("product_findById"));
            
            // Get all images
            Timer.Sample dbImageSample = Timer.start();
            List<Image> images = imageRepository.findByProductId(productId);
            dbImageSample.stop(metricsConfig.getDatabaseTimer("image_findByProductId"));
            
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (Image img : images) {
                Map<String, Object> map = new HashMap<>();
                map.put("image_id", img.getImageId());
                map.put("product_id", img.getProductId());
                map.put("file_name", img.getFileName());
                map.put("date_created", img.getDateCreated().toString());
                map.put("s3_bucket_path", img.getS3BucketPath());
                response.add(map);
            }
            
            log.info("GET /v1/product/{}/image - Retrieved {} images", productId, images.size());
            return ResponseEntity.ok(response);
            
        } finally {
            sample.stop(metricsConfig.getApiTimer("GET_v1_product_images"));
        }
    }
    
    @DeleteMapping("/{imageId}")
    public ResponseEntity<?> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            Authentication authentication) { 
        
        log.info("DELETE /v1/product/{}/image/{} - Deleting image by user: {}", 
                 productId, imageId, authentication.getName());
        
        Counter counter = metricsConfig.getApiCounter("DELETE_v1_product_image");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            // Get current user
            String username = authentication.getName();
            Timer.Sample dbUserSample = Timer.start();
            User user = userRepository.findByUsername(username);
            dbUserSample.stop(metricsConfig.getDatabaseTimer("user_findByUsername"));
            
            if (user == null) {
                log.error("DELETE /v1/product/{}/image/{} - User not found: {}", 
                          productId, imageId, username);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
            }
            
            // 加入驗證檢查
            if (!user.isVerified()) {
                log.warn("DELETE /v1/product/{}/image/{} - Email not verified: {}", 
                        productId, imageId, user.getUsername());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Email not verified. Please verify your email address before deleting images.");
            }
            
            // Verify image exists
            Timer.Sample dbImageSample = Timer.start();
            Image image = imageRepository.findById(imageId)
                    .orElseThrow(() -> {
                        log.warn("DELETE /v1/product/{}/image/{} - Image not found", productId, imageId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
                    });
            dbImageSample.stop(metricsConfig.getDatabaseTimer("image_findById"));
            
            // Verify image belongs to the product
            if (!image.getProductId().equals(productId)) {
                log.warn("DELETE /v1/product/{}/image/{} - Image does not belong to product", 
                         productId, imageId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
            }
            
            // Verify product exists
            Timer.Sample dbProductSample = Timer.start();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.warn("DELETE /v1/product/{}/image/{} - Product not found", productId, imageId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
                    });
            dbProductSample.stop(metricsConfig.getDatabaseTimer("product_findById"));
            
            // Verify user owns the product and image
            if (!image.getUserId().equals(user.getId()) || 
                !product.getOwnerUserId().equals(user.getId())) {
                log.warn("DELETE /v1/product/{}/image/{} - Access denied for user: {}", 
                         productId, imageId, username);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only delete images from your own products"));
            }
            
            // Delete from S3
            Timer.Sample s3Sample = Timer.start();
            s3Service.deleteFile(image.getS3BucketPath());
            s3Sample.stop(metricsConfig.getS3Timer("image_delete"));
            
            log.debug("DELETE /v1/product/{}/image/{} - File deleted from S3: {}", 
                      productId, imageId, image.getS3BucketPath());
            
            // Delete from database
            Timer.Sample dbDeleteSample = Timer.start();
            imageRepository.delete(image);
            dbDeleteSample.stop(metricsConfig.getDatabaseTimer("image_delete"));
            
            log.info("DELETE /v1/product/{}/image/{} - Image deleted successfully", productId, imageId);
            return ResponseEntity.noContent().build();
            
        } catch (ResponseStatusException e) {
            // Re-throw ResponseStatusException as-is
            throw e;
        } catch (Exception e) {
            log.error("DELETE /v1/product/{}/image/{} - Failed to delete image", 
                      productId, imageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete image"));
        } finally {
            sample.stop(metricsConfig.getApiTimer("DELETE_v1_product_image"));
        }
    }
}