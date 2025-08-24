package com.brokerage.service;

import com.brokerage.dto.CreateOrderRequest;
import com.brokerage.model.Order;
import com.brokerage.model.OrderSide;
import com.brokerage.model.OrderStatus;
import com.brokerage.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest buyOrderRequest;
    private CreateOrderRequest sellOrderRequest;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        buyOrderRequest = new CreateOrderRequest(
            "CUST001", "AAPL", OrderSide.BUY, 
            new BigDecimal("10"), new BigDecimal("150.00")
        );

        sellOrderRequest = new CreateOrderRequest(
            "CUST001", "AAPL", OrderSide.SELL, 
            new BigDecimal("5"), new BigDecimal("155.00")
        );

        testOrder = new Order(
            "CUST001", "AAPL", OrderSide.BUY, 
            new BigDecimal("10"), new BigDecimal("150.00")
        );
        testOrder.setId(1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setCreateDate(LocalDateTime.now());
    }

    @Test
    void testCreateOrder_BuyOrder() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order result = orderService.createOrder(buyOrderRequest);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals("CUST001", result.getCustomerId());
        assertEquals("AAPL", result.getAssetName());
        assertEquals(OrderSide.BUY, result.getOrderSide());
        
        verify(assetService).updateAssetForOrder(
            "CUST001", "AAPL", OrderSide.BUY, new BigDecimal("10")
        );
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testCreateOrder_SellOrder() {
        // Given
        Order sellOrder = new Order(
            "CUST001", "AAPL", OrderSide.SELL, 
            new BigDecimal("5"), new BigDecimal("155.00")
        );
        sellOrder.setId(2L);
        sellOrder.setStatus(OrderStatus.PENDING);
        sellOrder.setCreateDate(LocalDateTime.now());
        
        when(orderRepository.save(any(Order.class))).thenReturn(sellOrder);

        // When
        Order result = orderService.createOrder(sellOrderRequest);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(OrderSide.SELL, result.getOrderSide());
        
        verify(assetService).updateAssetForOrder(
            "CUST001", "AAPL", OrderSide.SELL, new BigDecimal("5")
        );
    }

    @Test
    void testGetCustomerOrders_WithDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<Order> expectedOrders = Arrays.asList(testOrder);
        
        when(orderRepository.findByCustomerIdAndCreateDateBetween("CUST001", startDate, endDate))
            .thenReturn(expectedOrders);

        // When
        List<Order> result = orderService.getCustomerOrders("CUST001", startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
    }

    @Test
    void testGetCustomerOrders_WithoutDateRange() {
        // Given
        List<Order> expectedOrders = Arrays.asList(testOrder);
        when(orderRepository.findByCustomerId("CUST001")).thenReturn(expectedOrders);

        // When
        List<Order> result = orderService.getCustomerOrders("CUST001", null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
    }

    @Test
    void testCancelOrder_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        assertDoesNotThrow(() -> orderService.cancelOrder(1L, "CUST001"));

        // Then
        verify(orderRepository).save(any(Order.class));
        verify(assetService).updateAssetForOrderCancellation(
            "CUST001", "AAPL", OrderSide.BUY, new BigDecimal("10")
        );
    }

    @Test
    void testCancelOrder_OrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(999L, "CUST001"));
        verify(assetService, never()).updateAssetForOrderCancellation(any(), any(), any(), any());
    }

    @Test
    void testCancelOrder_AlreadyMatched() {
        // Given
        testOrder.setStatus(OrderStatus.MATCHED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(1L, "CUST001"));
        verify(assetService, never()).updateAssetForOrderCancellation(any(), any(), any(), any());
    }

    @Test
    void testCancelOrder_WrongCustomer() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(1L, "WRONG_CUSTOMER"));
        verify(assetService, never()).updateAssetForOrderCancellation(any(), any(), any(), any());
    }

    @Test
    void testGetPendingOrders() {
        // Given
        List<Order> expectedOrders = Arrays.asList(testOrder);
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(expectedOrders);

        // When
        List<Order> result = orderService.getPendingOrders();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
    }

    @Test
    void testMatchOrder_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        assertDoesNotThrow(() -> orderService.matchOrder(1L));

        // Then
        verify(orderRepository).save(any(Order.class));
        verify(assetService).updateAssetForOrderMatching(
            "CUST001", "AAPL", OrderSide.BUY, new BigDecimal("10"), new BigDecimal("150.00")
        );
    }

    @Test
    void testMatchOrder_OrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.matchOrder(999L));
        verify(assetService, never()).updateAssetForOrderMatching(any(), any(), any(), any(), any());
    }

    @Test
    void testMatchOrder_AlreadyMatched() {
        // Given
        testOrder.setStatus(OrderStatus.MATCHED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.matchOrder(1L));
        verify(assetService, never()).updateAssetForOrderMatching(any(), any(), any(), any(), any());
    }

    @Test
    void testGetOrderById_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Order result = orderService.getOrderById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testOrder, result);
    }

    @Test
    void testGetOrderById_NotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.getOrderById(999L));
    }
} 