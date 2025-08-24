package com.brokerage.service;

import com.brokerage.model.Asset;
import com.brokerage.model.OrderSide;
import com.brokerage.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AssetService {
    
    @Autowired
    private AssetRepository assetRepository;
    
    public List<Asset> getCustomerAssets(String customerId) {
        return assetRepository.findByCustomerId(customerId);
    }
    
    public Asset getCustomerAsset(String customerId, String assetName) {
        return assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetName + " for customer: " + customerId));
    }
    
    @Transactional
    public void updateAssetForOrder(String customerId, String assetName, OrderSide orderSide, BigDecimal size) {
        Asset asset = getCustomerAsset(customerId, assetName);
        
        if (orderSide == OrderSide.BUY) {
            // For BUY orders, we need TRY (money) to purchase the asset
            Asset tryAsset = getCustomerAsset(customerId, "TRY");
            if (tryAsset.getUsableSize().compareTo(size) < 0) {
                throw new RuntimeException("Insufficient TRY balance for order");
            }
            tryAsset.setUsableSize(tryAsset.getUsableSize().subtract(size));
            assetRepository.save(tryAsset);
        } else {
            // For SELL orders, we need the asset to sell
            if (asset.getUsableSize().compareTo(size) < 0) {
                throw new RuntimeException("Insufficient asset balance for order");
            }
            asset.setUsableSize(asset.getUsableSize().subtract(size));
            assetRepository.save(asset);
        }
    }
    
    @Transactional
    public void updateAssetForOrderCancellation(String customerId, String assetName, OrderSide orderSide, BigDecimal size) {
        if (orderSide == OrderSide.BUY) {
            // Return TRY to customer
            Asset tryAsset = getCustomerAsset(customerId, "TRY");
            tryAsset.setUsableSize(tryAsset.getUsableSize().add(size));
            assetRepository.save(tryAsset);
        } else {
            // Return asset to customer
            Asset asset = getCustomerAsset(customerId, assetName);
            asset.setUsableSize(asset.getUsableSize().add(size));
            assetRepository.save(asset);
        }
    }
    
    @Transactional
    public void updateAssetForOrderMatching(String customerId, String assetName, OrderSide orderSide, BigDecimal size, BigDecimal price) {
        if (orderSide == OrderSide.BUY) {
            // Customer bought asset, add to their portfolio
            Asset asset = getCustomerAsset(customerId, assetName);
            asset.setSize(asset.getSize().add(size));
            asset.setUsableSize(asset.getUsableSize().add(size));
            assetRepository.save(asset);
        } else {
            // Customer sold asset, add TRY to their balance
            Asset tryAsset = getCustomerAsset(customerId, "TRY");
            BigDecimal totalValue = size.multiply(price);
            tryAsset.setSize(tryAsset.getSize().add(totalValue));
            tryAsset.setUsableSize(tryAsset.getUsableSize().add(totalValue));
            assetRepository.save(tryAsset);
        }
    }
    
    public void initializeCustomerAssets(String customerId) {
        // Initialize TRY asset for new customer
        Asset tryAsset = new Asset(customerId, "TRY", BigDecimal.ZERO, BigDecimal.ZERO);
        assetRepository.save(tryAsset);
    }
} 