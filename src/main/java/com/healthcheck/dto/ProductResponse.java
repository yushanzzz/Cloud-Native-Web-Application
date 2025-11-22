package com.healthcheck.dto;

import java.time.LocalDateTime;

import com.healthcheck.entity.Product;

public class ProductResponse {
    
    private Long id;
    private String name;
    private String description;
    private String sku;
    private String manufacturer;
    private Integer quantity;
    private LocalDateTime dateAdded;
    private LocalDateTime dateLastUpdated;
    private Long ownerUserId;
    
    public ProductResponse(){}

    public ProductResponse(Product product){

        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.sku = product.getSku();
        this.manufacturer = product.getManufacturer();
        this.quantity = product.getQuantity();
        this.dateAdded = product.getDateAdded();
        this.dateLastUpdated = product.getDateLastUpdated();
        this.ownerUserId = product.getOwnerUserId();
    }

    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public String getName() { 
        return name; 
    }
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getDescription() { 
        return description; 
    }
    public void setDescription(String description) { 
        this.description = description; 
    }
    
    public String getSku() { 
        return sku; 
    }
    public void setSku(String sku) { 
        this.sku = sku; 
    }
    
    public String getManufacturer() { 
        return manufacturer; 
    }
    public void setManufacturer(String manufacturer) { 
        this.manufacturer = manufacturer; 
    }
    
    public Integer getQuantity() { 
        return quantity; 
    }
    public void setQuantity(Integer quantity) { 
        this.quantity = quantity; 
    }
    
    public LocalDateTime getDateAdded() { 
        return dateAdded; 
    }
    public void setDateAdded(LocalDateTime dateAdded) { 
        this.dateAdded = dateAdded; 
    }
    
    public LocalDateTime getDateLastUpdated() {
        return dateLastUpdated; 
        }
    public void setDateLastUpdated(LocalDateTime dateLastUpdated) { 
        this.dateLastUpdated = dateLastUpdated; 
    }
    
    public Long getOwnerUserId() {
         return ownerUserId; 
        }
    public void setOwnerUserId(Long ownerUserId) { 
        this.ownerUserId = ownerUserId; 
    }
}
