package com.healthcheck.entity;


import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Product name is required")
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false, unique = true)
    @NotBlank(message = "SKU is required")
    private String sku;
    
    @Column(nullable = false)
    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;
    
    @Column(nullable = false)
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be less than 0")
    private Integer quantity;
    
    @Column(name = "date_added", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime dateAdded;
    
    @Column(name = "date_last_updated", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime dateLastUpdated;
    
    @Column(name = "owner_user_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long ownerUserId;
    
    
    public Product() {
    }
    
    public Product(String name, String description, String sku, String manufacturer, 
                   Integer quantity, Long ownerUserId) {
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.manufacturer = manufacturer;
        this.quantity = quantity;
        this.ownerUserId = ownerUserId;
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
    
   
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.dateAdded = now;
        this.dateLastUpdated = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.dateLastUpdated = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(sku, product.sku);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sku='" + sku + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", quantity=" + quantity +
                ", ownerUserId=" + ownerUserId +
                ", dateAdded=" + dateAdded +
                ", dateLastUpdated=" + dateLastUpdated +
                '}';
    }
}
