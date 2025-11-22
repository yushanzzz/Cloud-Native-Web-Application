package com.healthcheck.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthcheck.dto.ProductCreateRequest;
import com.healthcheck.dto.ProductResponse;
import com.healthcheck.entity.Product;
import com.healthcheck.repository.ProductRepository;

@Service
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Autowired
    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(ProductCreateRequest request, Long ownerUserId) {
        
        // Check if product with SKU already exists
        if (productRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Product with SKU " + request.getSku() + " already exists");
        }
        
        // Create new product entity
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setManufacturer(request.getManufacturer());
        product.setQuantity(request.getQuantity());
        product.setOwnerUserId(ownerUserId);
        
        // Save product to database
        Product savedProduct = productRepository.save(product);
        
        return new ProductResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        return new ProductResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductByIdAndOwner(Long productId, Long ownerUserId) {
        // 先檢查產品是否存在
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found");
        }
        
        // 再檢查所有權
        Product product = productRepository.findByIdAndOwnerUserId(productId, ownerUserId)
                .orElseThrow(() -> new RuntimeException("Access denied"));
        
        return new ProductResponse(product);
    }

    public ProductResponse updateProduct(Long productId, ProductCreateRequest request, Long ownerUserId) {
         // 步驟1：先檢查產品是否存在
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found");
        }
        Product product = productRepository.findByIdAndOwnerUserId(productId, ownerUserId)
            .orElseThrow(() -> new RuntimeException("Access denied"));
        
        // 檢查 SKU 重複
        if (productRepository.existsBySkuAndIdNot(request.getSku(), productId)) {
            throw new RuntimeException("Another product with SKU " + request.getSku() + " already exists");
        }
        
        // 更新字段
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setManufacturer(request.getManufacturer());
        product.setQuantity(request.getQuantity());
        
        Product updatedProduct = productRepository.save(product);
        return new ProductResponse(updatedProduct);
    }
    
    public ProductResponse patchProduct(Long productId, ProductCreateRequest request, Long ownerUserId) {
        // 回復到原來的版本，不要分離檢查
        Product product = productRepository.findByIdAndOwnerUserId(productId, ownerUserId)
            .orElseThrow(() -> new RuntimeException("Product not found or access denied"));
        
        // Update only non-null fields
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            product.setName(request.getName());
        }
        
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        
        if (request.getSku() != null && !request.getSku().trim().isEmpty()) {
            if (productRepository.existsBySkuAndIdNot(request.getSku(), productId)) {
                throw new RuntimeException("Another product with SKU " + request.getSku() + " already exists");
            }
            product.setSku(request.getSku());
        }
        
        if (request.getManufacturer() != null && !request.getManufacturer().trim().isEmpty()) {
            product.setManufacturer(request.getManufacturer());
        }
        
        if (request.getQuantity() != null) {
            product.setQuantity(request.getQuantity());
        }
        
        Product updatedProduct = productRepository.save(product);
        return new ProductResponse(updatedProduct);
    }
    public void deleteProduct(Long productId, Long ownerUserId) {

        // 先檢查產品是否存在
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found");
        }
        // 一次查詢，不分離檢查
        Product product = productRepository.findByIdAndOwnerUserId(productId, ownerUserId)
            .orElseThrow(() -> new RuntimeException("Product not found or access denied"));
        
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByOwner(Long ownerUserId) {
        List<Product> products = productRepository.findByOwnerUserId(ownerUserId);
        return products.stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isProductOwnedByUser(Long productId, Long ownerUserId) {
        return productRepository.findByIdAndOwnerUserId(productId, ownerUserId).isPresent();
    }
}