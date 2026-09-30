package com.growthpilot.service;

import com.growthpilot.dto.AuthRequest;
import com.growthpilot.dto.AuthResponse;
import com.growthpilot.dto.UserProfileDto;
import com.growthpilot.entity.Business;
import com.growthpilot.entity.User;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.BusinessRepository;
import com.growthpilot.repository.UserRepository;
import com.growthpilot.security.JwtUtil;
import com.growthpilot.security.SecurityUtils;
import com.growthpilot.dto.PasswordResetDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static class OtpEntry {
        final String code;
        final LocalDateTime expiresAt;
        boolean verified;

        OtpEntry(String code, LocalDateTime expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.verified = false;
        }
    }

    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final SecurityUtils securityUtils;

    public AuthService(UserRepository userRepository, BusinessRepository businessRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                       SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        String businessName = (request.getBusinessName() != null && !request.getBusinessName().isBlank())
                ? request.getBusinessName()
                : "My E-Commerce Store";

        Business business = Business.builder()
                .name(businessName)
                .description("E-Commerce Growth Workspace")
                .createdAt(LocalDateTime.now())
                .build();
        business = businessRepository.save(business);

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .business(business)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .businessId(business.getId())
                .businessName(business.getName())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        Business business = user.getBusiness();
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .businessId(business != null ? business.getId() : null)
                .businessName(business != null ? business.getName() : "GrowthPilot Store")
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile() {
        User user = securityUtils.getCurrentUser();
        Business business = user.getBusiness();
        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .businessId(business != null ? business.getId() : null)
                .businessName(business != null ? business.getName() : null)
                .businessDescription(business != null ? business.getDescription() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    public PasswordResetDtos.PasswordResetResponse sendResetCode(PasswordResetDtos.ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));

        // Generate 6-digit secure numeric verification OTP
        int randomNum = 100000 + SECURE_RANDOM.nextInt(900000);
        String code = String.valueOf(randomNum);

        // Store OTP with 10-minute expiry
        otpStorage.put(email, new OtpEntry(code, LocalDateTime.now().plusMinutes(10)));
        log.info("Password reset OTP generated for {}: {}", email, code);

        return PasswordResetDtos.PasswordResetResponse.builder()
                .success(true)
                .message("A 6-digit verification code has been dispatched to " + email)
                .email(email)
                .demoOtp(code)
                .build();
    }

    public PasswordResetDtos.PasswordResetResponse verifyResetCode(PasswordResetDtos.VerifyCodeRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Verification code is required.");
        }

        String email = request.getEmail().trim().toLowerCase();
        String code = request.getCode().trim();

        OtpEntry entry = otpStorage.get(email);
        if (entry == null || entry.expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired or was not requested. Please request a new code.");
        }

        if (!entry.code.equals(code)) {
            throw new IllegalArgumentException("Invalid verification code. Please check and try again.");
        }

        entry.verified = true;
        log.info("Password reset OTP verified successfully for {}", email);

        return PasswordResetDtos.PasswordResetResponse.builder()
                .success(true)
                .message("Verification code verified successfully.")
                .email(email)
                .build();
    }

    @Transactional
    public PasswordResetDtos.PasswordResetResponse resetPassword(PasswordResetDtos.ResetPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        String email = request.getEmail().trim().toLowerCase();
        OtpEntry entry = otpStorage.get(email);
        if (entry == null || !entry.verified || entry.expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification session expired or unverified. Please verify your email first.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Remove OTP entry once password updated
        otpStorage.remove(email);
        log.info("Password successfully updated for user {}", email);

        return PasswordResetDtos.PasswordResetResponse.builder()
                .success(true)
                .message("Your password has been reset successfully. You can now sign in with your new password.")
                .email(email)
                .build();
    }
}
