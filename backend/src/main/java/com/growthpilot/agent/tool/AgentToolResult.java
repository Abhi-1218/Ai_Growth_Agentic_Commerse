package com.growthpilot.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolResult {
    private boolean success;
    private Object data;
    private String summary;
    private String error;

    public static AgentToolResult ok(Object data, String summary) {
        return AgentToolResult.builder()
                .success(true)
                .data(data)
                .summary(summary)
                .build();
    }

    public static AgentToolResult fail(String error) {
        return AgentToolResult.builder()
                .success(false)
                .error(error)
                .summary("Tool execution failed: " + error)
                .build();
    }
}
