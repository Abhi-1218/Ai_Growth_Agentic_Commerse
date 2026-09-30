package com.growthpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataPoint {
    private String date; // e.g. "2026-08-01" or "Week 1"
    private BigDecimal revenue;
    private long orders;
}
