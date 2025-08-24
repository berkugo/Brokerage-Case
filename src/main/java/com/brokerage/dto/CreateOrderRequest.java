package com.brokerage.dto;

import com.brokerage.model.OrderSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class CreateOrderRequest {
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotBlank(message = "Asset name is required")
    private String assetName;
    
    @NotNull(message = "Order side is required")
    private OrderSide orderSide;
    
    @NotNull(message = "Size is required")
    @Positive(message = "Size must be positive")
    private BigDecimal size;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    
    // Constructors
    public CreateOrderRequest() {}
    
    public CreateOrderRequest(String customerId, String assetName, OrderSide orderSide, 
                            BigDecimal size, BigDecimal price) {
        this.customerId = customerId;
        this.assetName = assetName;
        this.orderSide = orderSide;
        this.size = size;
        this.price = price;
    }
    
    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getAssetName() {
        return assetName;
    }
    
    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }
    
    public OrderSide getOrderSide() {
        return orderSide;
    }
    
    public void setOrderSide(OrderSide orderSide) {
        this.orderSide = orderSide;
    }
    
    public BigDecimal getSize() {
        return size;
    }
    
    public void setSize(BigDecimal size) {
        this.size = size;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
} 