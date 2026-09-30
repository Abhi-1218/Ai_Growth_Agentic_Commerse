package com.growthpilot.agent.tool;

import java.util.Map;

public interface AgentTool {
    String getName();
    String getDescription();
    String getParameterSchema();
    boolean requiresHumanApproval();
    AgentToolResult execute(Map<String, Object> parameters, Long businessId, Long userId);
}
