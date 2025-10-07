package org.laioffer.planner.planning.ai.model;

import java.util.List;

/**
 * Response DTO received from the AI model containing the generated travel plan.
 * AiPlanResponse.java - AI 返回的响应
 */
public class AiPlanResponse {

    private List<AiPlannedDay> days;
    private String summary;

    // Constructors
    public AiPlanResponse() {}

    // Getters and Setters
    public List<AiPlannedDay> getDays() {
        return days;
    }

    public void setDays(List<AiPlannedDay> days) {
        this.days = days;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
