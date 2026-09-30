package com.growthpilot.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentToolRegistry.class);
    private final Map<String, AgentTool> tools = new HashMap<>();

    public AgentToolRegistry(List<AgentTool> registeredTools) {
        for (AgentTool tool : registeredTools) {
            tools.put(tool.getName(), tool);
            log.info("Registered AI Agent Tool: '{}'", tool.getName());
        }
    }

    public Collection<AgentTool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    public Optional<AgentTool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public AgentToolResult invokeTool(String toolName, Map<String, Object> parameters, Long businessId, Long userId) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            return AgentToolResult.fail("Tool '" + toolName + "' is not registered or allowed.");
        }
        try {
            return tool.execute(parameters, businessId, userId);
        } catch (Exception e) {
            log.error("Tool execution failed for {}: {}", toolName, e.getMessage());
            return AgentToolResult.fail(e.getMessage());
        }
    }

    public String buildToolDefinitionsPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Internal Tools:\n");
        for (AgentTool tool : tools.values()) {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription())
              .append(" (Schema: ").append(tool.getParameterSchema()).append(")\n");
        }
        return sb.toString();
    }
}
