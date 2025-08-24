package com.brokerage.service;

import com.brokerage.dto.CreateOrderRequest;
import com.brokerage.model.Order;
import com.brokerage.model.OrderSide;
import com.brokerage.model.OrderStatus;
import com.brokerage.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private AssetService assetService;
    
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // Validate asset availability before creating order
        if (request.getOrderSide() == OrderSide.BUY) {
            // For BUY orders, check if customer has enough TRY
            assetService.getCustomerAsset(request.getCustomerId(), "TRY");
        } else {
            // For SELL orders, check if customer has enough of the asset
            assetService.getCustomerAsset(request.getCustomerId(), request.getAssetName());
        }
        
        // Create the order
        Order order = new Order(
            request.getCustomerId(),
            request.getAssetName(),
            request.getOrderSide(),
            request.getSize(),
            request.getPrice()
        );
        
        // Update assets (reserve TRY or asset)
        assetService.updateAssetForOrder(
            request.getCustomerId(),
            request.getAssetName(),
            request.getOrderSide(),
            request.getSize()
        );
        
        return orderRepository.save(order);
    }
    
    public List<Order> getCustomerOrders(String customerId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return orderRepository.findByCustomerIdAndCreateDateBetween(customerId, startDate, endDate);
        } else if (startDate != null) {
            return orderRepository.findByCustomerIdAndCreateDateBetween(customerId, startDate, LocalDateTime.now());
        } else {
            return orderRepository.findByCustomerId(customerId);
        }
    }
    
    @Transactional
    public void cancelOrder(Long orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Check if order belongs to customer (unless admin)
        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Order does not belong to customer");
        }
        
        // Check if order can be cancelled
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be cancelled");
        }
        
        // Update order status
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        
        // Return assets to customer
        assetService.updateAssetForOrderCancellation(
            order.getCustomerId(),
            order.getAssetName(),
            order.getOrderSide(),
            order.getSize()
        );
    }
    
    public List<Order> getPendingOrders() {
        return orderRepository.findByStatus(OrderStatus.PENDING);
    }
    
    @Transactional
    public void matchOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be matched");
        }
        
        // Update order status
        order.setStatus(OrderStatus.MATCHED);
        orderRepository.save(order);
        
        // Update assets based on order execution
        assetService.updateAssetForOrderMatching(
            order.getCustomerId(),
            order.getAssetName(),
            order.getOrderSide(),
            order.getSize(),
            order.getPrice()
        );
    }
    
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
} 