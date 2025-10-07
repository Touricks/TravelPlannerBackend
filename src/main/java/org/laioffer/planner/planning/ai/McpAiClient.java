package org.laioffer.planner.planning.ai;

import org.laioffer.planner.planning.ai.model.AiPlanRequest;
import org.laioffer.planner.planning.ai.model.AiPlanResponse;

/**
 * Interface for communicating with the MCP AI service to generate travel plans.
 * McpAiClient.java - AI 客户端接口
 */
public interface McpAiClient {

    /**
     * Sends a planning request to the AI model and returns the generated plan.
     *
     * @param request The AI planning request containing itinerary and place details
     * @return The AI-generated travel plan
     */
    AiPlanResponse generatePlan(AiPlanRequest request);
}
