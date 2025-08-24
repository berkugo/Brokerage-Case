package com.brokerage.config;

import com.brokerage.model.Asset;
import com.brokerage.model.User;
import com.brokerage.service.AssetService;
import com.brokerage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AssetService assetService;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            userService.createUser("admin", "admin123", User.UserRole.ADMIN, null);
            System.out.println("Admin user created successfully");
        } catch (Exception e) {
            System.out.println("Admin user already exists or error: " + e.getMessage());
        }
        
        try {
            userService.createUser("customer1", "customer123", User.UserRole.CUSTOMER, "CUST001");
            System.out.println("Sample customer created successfully");
            
            assetService.initializeCustomerAssets("CUST001");
            
            Asset tryAsset = assetService.getCustomerAsset("CUST001", "TRY");
            tryAsset.setSize(new BigDecimal("10000.00"));
            tryAsset.setUsableSize(new BigDecimal("10000.00"));
            // Note: In a real application, ı would use a proper service method to update this
            
        } catch (Exception e) {
            System.out.println("Sample customer already exists or error: " + e.getMessage());
        }
        
        try {
            userService.createUser("customer2", "customer456", User.UserRole.CUSTOMER, "CUST002");
            System.out.println("Second sample customer created successfully");
            
            assetService.initializeCustomerAssets("CUST002");
            
            Asset tryAsset = assetService.getCustomerAsset("CUST002", "TRY");
            tryAsset.setSize(new BigDecimal("5000.00"));
            tryAsset.setUsableSize(new BigDecimal("5000.00"));
            
        } catch (Exception e) {
            System.out.println("Second sample customer already exists or error: " + e.getMessage());
        }
    }
} 