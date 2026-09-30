package com.growthpilot.controller;

import com.growthpilot.dto.AgentActionDto;
import com.growthpilot.dto.AiChatRequest;
import com.growthpilot.dto.AiChatResponse;
import com.growthpilot.entity.AgentAction;
import com.growthpilot.service.AgentActionService;
import com.growthpilot.service.AiCopilotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "AI Commerce Agent & Copilot", description = "Autonomous commerce growth agent, conversational copilot, and human-in-the-loop action approval workflow")
public class AgentController {

    private final AiCopilotService aiCopilotService;
    private final AgentActionService agentActionService;

    public AgentController(AiCopilotService aiCopilotService, AgentActionService agentActionService) {
        this.aiCopilotService = aiCopilotService;
        this.agentActionService = agentActionService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Send a growth inquiry to AI Copilot and receive data-backed response with proposed actions")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiCopilotService.chat(request));
    }

    @GetMapping("/actions")
    @Operation(summary = "Get audit log of all AI agent actions with status filtering (PENDING_APPROVAL, APPROVED, REJECTED, EXECUTED)")
    public ResponseEntity<Page<AgentActionDto>> getActions(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(agentActionService.getActionsDto(status, pageable));
    }

    @PostMapping("/actions/{id}/approve")
    @Operation(summary = "Approve and execute a proposed AI agent growth action")
    public ResponseEntity<AgentActionDto> approveAction(@PathVariable Long id) {
        AgentAction action = agentActionService.approve(id);
        return ResponseEntity.ok(agentActionService.toDto(action));
    }

    @PostMapping("/actions/{id}/reject")
    @Operation(summary = "Reject a proposed AI agent action")
    public ResponseEntity<AgentActionDto> rejectAction(@PathVariable Long id) {
        AgentAction action = agentActionService.reject(id);
        return ResponseEntity.ok(agentActionService.toDto(action));
    }
}
