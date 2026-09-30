package com.growthpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String response;
    private List<ProposedActionDto> proposedActions;
    private List<String> toolsInvoked;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProposedActionDto {
        private String title;
        private String actionType; // OFFER, CAMPAIGN, CART_RECOVERY, RETENTION_EMAIL
        private String description;
        private String estimatedImpact;
        private String targetEntity;
        private String suggestedParameters;
        private boolean requiresApproval;
    }
}
