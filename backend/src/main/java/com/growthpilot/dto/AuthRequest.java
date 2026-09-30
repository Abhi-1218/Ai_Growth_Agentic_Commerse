package com.growthpilot.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
    private String businessName; // Used for registration
}
