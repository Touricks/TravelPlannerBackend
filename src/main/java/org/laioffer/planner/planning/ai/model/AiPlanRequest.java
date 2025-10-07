package org.laioffer.planner.planning.ai.model;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Request DTO sent to the AI model for generating a travel plan.
 * AiPlanRequest.java - 发送给 AI 的请求，包含行程信息和感兴趣的地点
 */
public class AiPlanRequest {

    private String destinationCity;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private String travelMode;
    private Integer budgetInCents;
    private LocalTime dailyStart;
    private LocalTime dailyEnd;
    private List<AiPlaceInfo> interestedPlaces;

    // Constructors
    public AiPlanRequest() {}

    // Getters and Setters
    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public String getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(String travelMode) {
        this.travelMode = travelMode;
    }

    public Integer getBudgetInCents() {
        return budgetInCents;
    }

    public void setBudgetInCents(Integer budgetInCents) {
        this.budgetInCents = budgetInCents;
    }

    public LocalTime getDailyStart() {
        return dailyStart;
    }

    public void setDailyStart(LocalTime dailyStart) {
        this.dailyStart = dailyStart;
    }

    public LocalTime getDailyEnd() {
        return dailyEnd;
    }

    public void setDailyEnd(LocalTime dailyEnd) {
        this.dailyEnd = dailyEnd;
    }

    public List<AiPlaceInfo> getInterestedPlaces() {
        return interestedPlaces;
    }

    public void setInterestedPlaces(List<AiPlaceInfo> interestedPlaces) {
        this.interestedPlaces = interestedPlaces;
    }
}
