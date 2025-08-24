package com.brokerage.controller;

import com.brokerage.model.Asset;
import com.brokerage.model.User;
import com.brokerage.service.AssetService;
import com.brokerage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
public class AssetController {
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<List<Asset>> getCustomerAssets(@RequestParam String customerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByUsername(auth.getName());
        
        if (currentUser.getRole() == User.UserRole.CUSTOMER && 
            !currentUser.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Access denied: Can only view your own assets.");
        }
        
        List<Asset> assets = assetService.getCustomerAssets(customerId);
        return ResponseEntity.ok(assets);
    }
    
    @GetMapping("/{assetName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<Asset> getCustomerAsset(
            @RequestParam String customerId,
            @PathVariable String assetName) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByUsername(auth.getName());
        
        if (currentUser.getRole() == User.UserRole.CUSTOMER && 
            !currentUser.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Access denied: Can only view your own assets.");
        }
        
        Asset asset = assetService.getCustomerAsset(customerId, assetName);
        return ResponseEntity.ok(asset);
    }
} 