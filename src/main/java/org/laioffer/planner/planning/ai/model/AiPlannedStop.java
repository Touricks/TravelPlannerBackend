package org.laioffer.planner.planning.ai.model;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a single stop/place in a day's itinerary.
 * AiPlannedStop.java - AI 生成的每个站点详情
 */
public class AiPlannedStop {

    private UUID placeId;
    private String placeName;
    private LocalTime arrivalTime;
    private LocalTime departureTime;
    private Integer durationMinutes;
    private String activity;
    private String transportMode;
    private Integer transportDurationMinutes;

    // Constructors
    public AiPlannedStop() {}

    // Getters and Setters
    public UUID getPlaceId() {
        return placeId;
    }

    public void setPlaceId(UUID placeId) {
        this.placeId = placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public Integer getTransportDurationMinutes() {
        return transportDurationMinutes;
    }

    public void setTransportDurationMinutes(Integer transportDurationMinutes) {
        this.transportDurationMinutes = transportDurationMinutes;
    }
}
