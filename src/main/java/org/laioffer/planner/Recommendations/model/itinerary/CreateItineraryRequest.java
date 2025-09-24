package org.laioffer.planner.Recommendations.model.itinerary;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateItineraryRequest {
    private String destinationCity;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime endDate;
    
    private TravelMode travelMode;
    private Integer budgetLimitCents;
    private String dailyStart;
    private String dailyEnd;
    
    public CreateItineraryRequest() {}
    
    public CreateItineraryRequest(String destinationCity, OffsetDateTime startDate, OffsetDateTime endDate) {
        this.destinationCity = destinationCity;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
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
    
    public TravelMode getTravelMode() {
        return travelMode;
    }
    
    public void setTravelMode(TravelMode travelMode) {
        this.travelMode = travelMode;
    }
    
    public Integer getBudgetLimitCents() {
        return budgetLimitCents;
    }
    
    public void setBudgetLimitCents(Integer budgetLimitCents) {
        this.budgetLimitCents = budgetLimitCents;
    }
    
    public String getDailyStart() {
        return dailyStart;
    }
    
    public void setDailyStart(String dailyStart) {
        this.dailyStart = dailyStart;
    }
    
    public String getDailyEnd() {
        return dailyEnd;
    }
    
    public void setDailyEnd(String dailyEnd) {
        this.dailyEnd = dailyEnd;
    }
    
    @Override
    public String toString() {
        return "CreateItineraryRequest{" +
                "destinationCity='" + destinationCity + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", travelMode=" + travelMode +
                ", budgetLimitCents=" + budgetLimitCents +
                ", dailyStart='" + dailyStart + '\'' +
                ", dailyEnd='" + dailyEnd + '\'' +
                '}';
    }
}