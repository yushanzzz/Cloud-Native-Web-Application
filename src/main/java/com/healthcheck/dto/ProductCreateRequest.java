package com.healthcheck.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProductCreateRequest {
    
    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be less than 0")
    private Integer quantity;

    public ProductCreateRequest(){}

    public ProductCreateRequest(String name, String description, String sku, 
                                String manufacturer, Integer quantity){
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.manufacturer = manufacturer;
        this.quantity = quantity;                            
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
}
