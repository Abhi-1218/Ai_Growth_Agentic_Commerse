package com.growthpilot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CustomerAuthDtos {
    private CustomerAuthDtos() {}
    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 100) String password, String phone) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record Profile(String customerId, Long id, String name, String email, String phone) {}
    public record AuthResponse(String token, String customerId, Long id, String name, String email) {}
    public record ProfileUpdate(@NotBlank String name, String phone) {}
}
