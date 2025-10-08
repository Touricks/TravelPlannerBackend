package org.laioffer.planner.planning.ai;

import org.laioffer.planner.planning.ai.model.AiPlanRequest;
import org.laioffer.planner.planning.ai.model.AiPlanResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of McpAiClient that communicates with the MCP AI service via HTTP.
 * McpAiClientImpl.java - AI 客户端实现，使用 RestTemplate 调用 MCP AI 服务
 */
@Component
public class McpAiClientImpl implements McpAiClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public McpAiClientImpl(
            RestTemplate restTemplate,
            @Value("${ai.mcp.service.url:http://localhost:8080/ai/plan}") String aiServiceUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }

    @Override
    public AiPlanResponse generatePlan(AiPlanRequest request) {
        // Call the AI service endpoint and return the response
        return restTemplate.postForObject(aiServiceUrl, request, AiPlanResponse.class);
    }
}
