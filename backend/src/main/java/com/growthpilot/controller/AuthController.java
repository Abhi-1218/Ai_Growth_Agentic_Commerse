package com.growthpilot.controller;

import com.growthpilot.dto.AuthRequest;
import com.growthpilot.dto.AuthResponse;
import com.growthpilot.dto.PasswordResetDtos;
import com.growthpilot.dto.UserProfileDto;
import com.growthpilot.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication & Workspace", description = "User registration, login, and workspace profile endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user and create an e-commerce workspace")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT access token")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request email verification code for forgotten password")
    public ResponseEntity<PasswordResetDtos.PasswordResetResponse> forgotPassword(
            @RequestBody PasswordResetDtos.ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.sendResetCode(request));
    }

    @PostMapping("/verify-code")
    @Operation(summary = "Verify code sent to email")
    public ResponseEntity<PasswordResetDtos.PasswordResetResponse> verifyCode(
            @RequestBody PasswordResetDtos.VerifyCodeRequest request) {
        return ResponseEntity.ok(authService.verifyResetCode(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset old password with a new verified password")
    public ResponseEntity<PasswordResetDtos.PasswordResetResponse> resetPassword(
            @RequestBody PasswordResetDtos.ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user and workspace profile")
    public ResponseEntity<UserProfileDto> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUserProfile());
    }
}

