package com.brokerage.security;

import com.brokerage.dto.LoginRequest;
import com.brokerage.dto.LoginResponse;
import com.brokerage.model.User;
import com.brokerage.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testAdminLogin_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        // Verify token is valid
        String responseContent = result.getResponse().getContentAsString();
        LoginResponse response = objectMapper.readValue(responseContent, LoginResponse.class);
        
        assertNotNull(response.getToken());
        assertTrue(tokenProvider.validateToken(response.getToken()));
        assertEquals("admin", tokenProvider.getUsernameFromJWT(response.getToken()));
    }

    @Test
    void testCustomerLogin_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest("customer1", "customer123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("customer1"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        LoginResponse response = objectMapper.readValue(responseContent, LoginResponse.class);
        
        assertNotNull(response.getToken());
        assertTrue(tokenProvider.validateToken(response.getToken()));
        assertEquals("customer1", tokenProvider.getUsernameFromJWT(response.getToken()));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden()); 
    }

    @Test
    void testLogin_NonExistentUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent", "password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden()); 
    }

    @Test
    void testRegisterNewCustomer_Success() throws Exception {
        LoginRequest registerRequest = new LoginRequest("newcustomer", "password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newcustomer"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerId").exists());

        // Verifyingf user was created in database
        User createdUser = userService.findByUsername("newcustomer");
        assertNotNull(createdUser);
        assertEquals("CUSTOMER", createdUser.getRole().toString());
        assertNotNull(createdUser.getCustomerId());
    }

    @Test
    void testRegister_DuplicateUsername() throws Exception {
        LoginRequest registerRequest = new LoginRequest("admin", "newpassword");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest()) 
                .andExpect(jsonPath("$.error").value("Username already exists."));
    }

    @Test
    void testJWTTokenValidation_ValidToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("customer1", "customer123");
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
                
        String responseContent = loginResult.getResponse().getContentAsString();
        LoginResponse response = objectMapper.readValue(responseContent, LoginResponse.class);
        String token = response.getToken();

        mockMvc.perform(get("/api/assets")
                .param("customerId", "CUST001")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testJWTTokenValidation_InvalidToken() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(get("/api/assets")
                .param("customerId", "CUST001")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isForbidden()); 
    }

    @Test
    void testJWTTokenValidation_ExpiredToken() throws Exception { 

        String expiredToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjdXN0b21lcjEiLCJpYXQiOjE2MzAwMDAwMDAsImV4cCI6MTYzMDAwMDAwMH0.invalid";

        mockMvc.perform(get("/api/assets")
                .param("customerId", "CUST001")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden()); 
    }

    @Test
    void testEndpointAccess_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/assets")
                .param("customerId", "CUST001"))
                .andExpect(status().isForbidden()); 
    }
}
