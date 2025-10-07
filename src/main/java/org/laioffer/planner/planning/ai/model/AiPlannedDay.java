package org.laioffer.planner.planning.ai.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a single day in the AI-generated travel plan.
 * AiPlannedDay.java - AI 生成的每日计划
 */
public class AiPlannedDay {

    private LocalDate date;
    private String summary;
    private List<AiPlannedStop> stops;

    // Constructors
    public AiPlannedDay() {}

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<AiPlannedStop> getStops() {
        return stops;
    }

    public void setStops(List<AiPlannedStop> stops) {
        this.stops = stops;
    }
}
