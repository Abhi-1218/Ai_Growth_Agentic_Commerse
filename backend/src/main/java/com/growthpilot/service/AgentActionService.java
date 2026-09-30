package com.growthpilot.service;

import com.growthpilot.agent.tool.AgentToolRegistry;
import com.growthpilot.agent.tool.AgentToolResult;
import com.growthpilot.dto.AgentActionDto;
import com.growthpilot.entity.AgentAction;
import com.growthpilot.entity.User;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.AgentActionRepository;
import com.growthpilot.security.SecurityUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentActionService {

    private final AgentActionRepository agentActionRepository;
    private final SecurityUtils securityUtils;
    private final AgentToolRegistry toolRegistry;

    public AgentActionService(AgentActionRepository agentActionRepository,
                              SecurityUtils securityUtils,
                              @Lazy AgentToolRegistry toolRegistry) {
        this.agentActionRepository = agentActionRepository;
        this.securityUtils = securityUtils;
        this.toolRegistry = toolRegistry;
    }

    @Transactional(readOnly = true)
    public Page<AgentActionDto> getActionsDto(String status, Pageable pageable) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Page<AgentAction> actions = agentActionRepository.findAllByBusinessIdOrderByCreatedAtDesc(businessId, pageable);

        List<AgentActionDto> dtos = actions.getContent().stream()
                .filter(a -> status == null || status.isBlank() || a.getStatus().equalsIgnoreCase(status.trim()))
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, actions.getTotalElements());
    }

    @Transactional
    public AgentAction createAction(String goal, String tool, String parameters, String reasoning) {
        User user = securityUtils.getCurrentUser();
        AgentAction action = AgentAction.builder()
                .business(user.getBusiness())
                .user(user)
                .goal(goal)
                .toolUsed(tool)
                .parameters(parameters)
                .reasoningSummary(reasoning)
                .status("PENDING_APPROVAL")
                .createdAt(LocalDateTime.now())
                .build();
        return agentActionRepository.save(action);
    }

    @Transactional
    public AgentAction approve(Long id) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Long userId = securityUtils.getCurrentUser().getId();

        AgentAction action = agentActionRepository.findById(id)
                .filter(a -> a.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Action not found with id: " + id));

        // Execute tool if registered
        String toolName = action.getToolUsed();
        String resultSummary = "Action approved and executed.";
        if (toolName != null && toolRegistry.getTool(toolName).isPresent()) {
            Map<String, Object> params = parseParameters(action.getParameters());
            AgentToolResult toolResult = toolRegistry.invokeTool(toolName, params, businessId, userId);
            if (toolResult.isSuccess()) {
                action.setStatus("EXECUTED");
                resultSummary = toolResult.getSummary();
            } else {
                action.setStatus("FAILED");
                resultSummary = "Execution failed: " + toolResult.getError();
            }
        } else {
            action.setStatus("APPROVED");
        }

        action.setResult(resultSummary);
        return agentActionRepository.save(action);
    }

    @Transactional
    public AgentAction reject(Long id) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        AgentAction action = agentActionRepository.findById(id)
                .filter(a -> a.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Action not found with id: " + id));

        action.setStatus("REJECTED");
        action.setResult("Action rejected by growth manager.");
        return agentActionRepository.save(action);
    }

    public AgentActionDto toDto(AgentAction a) {
        return AgentActionDto.builder()
                .id(a.getId())
                .businessId(a.getBusiness().getId())
                .userEmail(a.getUser() != null ? a.getUser().getEmail() : "system@growthpilot.ai")
                .goal(a.getGoal())
                .reasoningSummary(a.getReasoningSummary())
                .toolUsed(a.getToolUsed())
                .parameters(a.getParameters())
                .result(a.getResult())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private Map<String, Object> parseParameters(String paramJson) {
        if (paramJson == null || paramJson.isBlank()) return Collections.emptyMap();
        Map<String, Object> map = new HashMap<>();
        // Simple JSON key-value parser for flat/nested string parameters
        try {
            String clean = paramJson.replace("{", "").replace("}", "").trim();
            for (String pair : clean.split(",")) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "").replace("'", "");
                    String val = kv[1].trim().replace("\"", "").replace("'", "");
                    if (val.matches("-?\\d+")) {
                        map.put(key, Long.parseLong(val));
                    } else if (val.matches("-?\\d+\\.\\d+")) {
                        map.put(key, Double.parseDouble(val));
                    } else if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
                        map.put(key, Boolean.parseBoolean(val));
                    } else {
                        map.put(key, val);
                    }
                }
            }
        } catch (Exception e) {
            map.put("raw", paramJson);
        }
        return map;
    }
}
