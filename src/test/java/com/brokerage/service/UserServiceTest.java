package com.brokerage.service;

import com.brokerage.model.User;
import com.brokerage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService - focuses on Bonus 1 authentication features
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User customerUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setPassword("encodedAdminPassword");
        adminUser.setRole(User.UserRole.ADMIN);
        adminUser.setCustomerId(null); 

        customerUser = new User();
        customerUser.setId(2L);
        customerUser.setUsername("customer1");
        customerUser.setPassword("encodedCustomerPassword");
        customerUser.setRole(User.UserRole.CUSTOMER);
        customerUser.setCustomerId("CUST001");
    }

    // ===== UserDetailsService TESTS =====

    @Test
    void testLoadUserByUsername_AdminUser_Success() {
        
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When
        UserDetails result = userService.loadUserByUsername("admin");

        // Then
        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertEquals("encodedAdminPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isCredentialsNonExpired());

        // Check authorities
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void testLoadUserByUsername_CustomerUser_Success() {
        // Given
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(customerUser));

        // When
        UserDetails result = userService.loadUserByUsername("customer1");

        // Then
        assertNotNull(result);
        assertEquals("customer1", result.getUsername());
        assertEquals("encodedCustomerPassword", result.getPassword());

        // Check authorities
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> 
            userService.loadUserByUsername("nonexistent"));
    }

    // ===== USER CREATION TESTS =====

    @Test
    void testCreateUser_NewCustomer_Success() {
        // Given
        String username = "newcustomer";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";
        String customerId = "CUST003";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.findByCustomerId(anyString())).thenReturn(Optional.empty());
        
        User savedUser = new User();
        savedUser.setUsername(username);
        savedUser.setPassword(encodedPassword);
        savedUser.setRole(User.UserRole.CUSTOMER);
        savedUser.setCustomerId(customerId);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser(username, rawPassword);

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(encodedPassword, result.getPassword());
        assertEquals(User.UserRole.CUSTOMER, result.getRole());
        assertNotNull(result.getCustomerId());
        assertTrue(result.getCustomerId().startsWith("CUST"));

        verify(assetService).initializeCustomerAssets(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateUsername() {
        // Given
        String username = "existinguser";
        String password = "password123";
        
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            userService.createUser(username, password));
        
        verify(userRepository, never()).save(any(User.class));
        verify(assetService, never()).initializeCustomerAssets(anyString());
    }

    @Test
    void testCreateUser_CustomerIdGeneration_HandlesCollision() {
        // Given
        String username = "newcustomer";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        
        when(userRepository.findByCustomerId(anyString()))
            .thenReturn(Optional.of(new User())) 
            .thenReturn(Optional.empty()); 

        User savedUser = new User();
        savedUser.setUsername(username);
        savedUser.setPassword(encodedPassword);
        savedUser.setRole(User.UserRole.CUSTOMER);
        savedUser.setCustomerId("CUST002"); 
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser(username, rawPassword);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCustomerId());
        verify(userRepository, times(2)).findByCustomerId(anyString()); 
    }

    // ===== FIND USER TESTS =====

    @Test
    void testFindByUsername_Success() {
        // Given
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(customerUser));

        // When
        User result = userService.findByUsername("customer1");

        // Then
        assertNotNull(result);
        assertEquals("customer1", result.getUsername());
        assertEquals(User.UserRole.CUSTOMER, result.getRole());
        assertEquals("CUST001", result.getCustomerId());
    }

    @Test
    void testFindByUsername_NotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            userService.findByUsername("nonexistent"));
    }

    @Test
    void testFindByCustomerId_Success() {
        // Given
        when(userRepository.findByCustomerId("CUST001")).thenReturn(Optional.of(customerUser));

        // When
        User result = userService.findByCustomerId("CUST001");

        // Then
        assertNotNull(result);
        assertEquals("customer1", result.getUsername());
        assertEquals("CUST001", result.getCustomerId());
    }

    @Test
    void testFindByCustomerId_NotFound() {
        // Given
        when(userRepository.findByCustomerId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            userService.findByCustomerId("NONEXISTENT"));
    }

    // ===== ROLE-BASED AUTHORITY TESTS =====

    @Test
    void testUserAuthorities_AdminRole() {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When
        UserDetails userDetails = userService.loadUserByUsername("admin");

        // Then
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertFalse(authorities.contains(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void testUserAuthorities_CustomerRole() {
        // Given
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(customerUser));

        // When
        UserDetails userDetails = userService.loadUserByUsername("customer1");

        // Then
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        assertFalse(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ===== PASSWORD ENCODING TESTS =====

    @Test
    void testPasswordEncoding_DuringUserCreation() {
        // Given
        String username = "testuser";
        String rawPassword = "plaintext123";
        String encodedPassword = "encoded_plaintext123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.findByCustomerId(anyString())).thenReturn(Optional.empty());
        
        User savedUser = new User();
        savedUser.setPassword(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser(username, rawPassword);

        // Then
        verify(passwordEncoder).encode(rawPassword);
        assertEquals(encodedPassword, result.getPassword());
        assertNotEquals(rawPassword, result.getPassword()); // Ensure it's encoded, not plain
    }

    // ===== CUSTOMER ID GENERATION TESTS =====

    @Test
    void testCustomerIdGeneration_UniqueGeneration() {
        // Given
        String username1 = "customer1";
        String username2 = "customer2";
        String password = "password123";

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.findByCustomerId(anyString())).thenReturn(Optional.empty());
        
        User savedUser1 = new User();
        savedUser1.setCustomerId("CUST001");
        User savedUser2 = new User();
        savedUser2.setCustomerId("CUST002");
        
        when(userRepository.save(any(User.class)))
            .thenReturn(savedUser1)
            .thenReturn(savedUser2);

        // When
        User result1 = userService.createUser(username1, password);
        User result2 = userService.createUser(username2, password);

        // Then
        assertNotNull(result1.getCustomerId());
        assertNotNull(result2.getCustomerId());
        assertTrue(result1.getCustomerId().startsWith("CUST"));
        assertTrue(result2.getCustomerId().startsWith("CUST"));

    }
}
