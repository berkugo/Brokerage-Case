package com.brokerage.repository;

import com.brokerage.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    
    List<Asset> findByCustomerId(String customerId);
    
    Optional<Asset> findByCustomerIdAndAssetName(String customerId, String assetName);
    
    @Query("SELECT a FROM Asset a WHERE a.customerId = :customerId AND a.assetName = :assetName")
    Optional<Asset> findAssetByCustomerAndName(@Param("customerId") String customerId, 
                                             @Param("assetName") String assetName);
} 