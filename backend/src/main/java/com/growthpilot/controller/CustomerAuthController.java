package com.growthpilot.controller;

import com.growthpilot.dto.CustomerAuthDtos;
import com.growthpilot.service.CustomerAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/customer-auth", "/api/customer/auth"})
public class CustomerAuthController {
    private final CustomerAuthService service;
    public CustomerAuthController(CustomerAuthService service) { this.service = service; }
    @PostMapping("/register")
    public ResponseEntity<CustomerAuthDtos.AuthResponse> register(@Valid @RequestBody CustomerAuthDtos.RegisterRequest r) {
        return ResponseEntity.ok(service.register(r));
    }
    @PostMapping("/login")
    public ResponseEntity<CustomerAuthDtos.AuthResponse> login(@Valid @RequestBody CustomerAuthDtos.LoginRequest r) {
        return ResponseEntity.ok(service.login(r));
    }
    @GetMapping("/me")
    public ResponseEntity<CustomerAuthDtos.Profile> me() { return ResponseEntity.ok(service.profile()); }
    @PutMapping("/me")
    public ResponseEntity<CustomerAuthDtos.Profile> update(@Valid @RequestBody CustomerAuthDtos.ProfileUpdate r) {
        return ResponseEntity.ok(service.update(r));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() { return ResponseEntity.noContent().build(); }
}
