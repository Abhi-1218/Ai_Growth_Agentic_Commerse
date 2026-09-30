package com.growthpilot.agent;

import com.growthpilot.agent.tool.AgentTool;
import com.growthpilot.agent.tool.AgentToolRegistry;
import com.growthpilot.agent.tool.AgentToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolRegistryTest {

    private AgentToolRegistry registry;

    static class DummyTool implements AgentTool {
        @Override
        public String getName() {
            return "dummyTool";
        }

        @Override
        public String getDescription() {
            return "A dummy tool for testing registry";
        }

        @Override
        public String getParameterSchema() {
            return "{\"type\":\"object\"}";
        }

        @Override
        public boolean requiresHumanApproval() {
            return true;
        }

        @Override
        public AgentToolResult execute(Map<String, Object> parameters, Long businessId, Long userId) {
            return AgentToolResult.ok(Map.of("status", "ok"), "Executed dummy successfully");
        }
    }

    @BeforeEach
    void setUp() {
        registry = new AgentToolRegistry(List.of(new DummyTool()));
    }

    @Test
    void testGetToolDefinitionsPrompt() {
        String prompt = registry.buildToolDefinitionsPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("dummyTool"));
    }

    @Test
    void testInvokeTool_Success() {
        AgentToolResult result = registry.invokeTool("dummyTool", Collections.emptyMap(), 1L, 1L);
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Executed dummy successfully", result.getSummary());
    }

    @Test
    void testInvokeTool_NotFound() {
        AgentToolResult result = registry.invokeTool("nonExistentTool", Collections.emptyMap(), 1L, 1L);
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not registered"));
    }
}
