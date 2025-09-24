package org.laioffer.planner.Recommendations.model.itinerary;

import org.laioffer.planner.Recommendations.model.common.PageMeta;
import org.laioffer.planner.Recommendations.model.place.PlaceDTO;

import java.util.List;
import java.util.UUID;

public class GetRecommendationsResponse {
    private UUID itineraryId;
    private List<PlaceDTO> items;
    private PageMeta page;
    
    public GetRecommendationsResponse() {}
    
    public GetRecommendationsResponse(UUID itineraryId, List<PlaceDTO> items, PageMeta page) {
        this.itineraryId = itineraryId;
        this.items = items;
        this.page = page;
    }
    
    public UUID getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(UUID itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public List<PlaceDTO> getItems() {
        return items;
    }
    
    public void setItems(List<PlaceDTO> items) {
        this.items = items;
    }
    
    public PageMeta getPage() {
        return page;
    }
    
    public void setPage(PageMeta page) {
        this.page = page;
    }
    
    @Override
    public String toString() {
        return "GetRecommendationsResponse{" +
                "itineraryId=" + itineraryId +
                ", items=" + items +
                ", page=" + page +
                '}';
    }
}