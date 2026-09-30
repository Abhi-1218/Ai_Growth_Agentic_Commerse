package com.growthpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfileDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
