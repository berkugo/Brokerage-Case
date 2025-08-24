package com.brokerage.service;

import com.brokerage.model.User;
import com.brokerage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AssetService assetService;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
    
    public User createUser(String username, String password, User.UserRole role, String customerId) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists.");
        }
        
        User user = new User(username, passwordEncoder.encode(password), role, customerId);
        return userRepository.save(user);
    }
    
    // Overload for creating customer users with auto-generated customer ID
    public User createUser(String username, String password) {
        String customerId = generateCustomerId();
        User user = createUser(username, password, User.UserRole.CUSTOMER, customerId);
        
        // Initialize customer assets (TRY balance)
        assetService.initializeCustomerAssets(customerId);
        
        return user;
    }
    
    private String generateCustomerId() {
        String customerId;
        do {
            customerId = "CUST" + String.format("%03d", (int) (Math.random() * 1000));
        } while (userRepository.findByCustomerId(customerId).isPresent());
        return customerId;
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
    
    public User findByCustomerId(String customerId) {
        return userRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
} 