package com.brokerage.controller;

import com.brokerage.dto.LoginRequest;
import com.brokerage.dto.LoginResponse;
import com.brokerage.model.User;
import com.brokerage.security.JwtTokenProvider;
import com.brokerage.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String jwt = tokenProvider.generateToken(authentication);
        User user = userService.findByUsername(loginRequest.getUsername());
        
        return ResponseEntity.ok(new LoginResponse(jwt, user));
    }
    
        @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody LoginRequest registerRequest) {
        try {
            User user = userService.createUser(registerRequest.getUsername(), registerRequest.getPassword());
            return ResponseEntity.ok(new LoginResponse(null, user)); 
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
} 