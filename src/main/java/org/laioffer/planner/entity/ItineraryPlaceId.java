package org.laioffer.planner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ItineraryPlaceId implements Serializable {
    
    @Column(name = "itinerary_id")
    private UUID itineraryId;
    
    @Column(name = "place_id")
    private UUID placeId;
    
    public ItineraryPlaceId() {}
    
    public ItineraryPlaceId(UUID itineraryId, UUID placeId) {
        this.itineraryId = itineraryId;
        this.placeId = placeId;
    }
    
    public UUID getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(UUID itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public UUID getPlaceId() {
        return placeId;
    }
    
    public void setPlaceId(UUID placeId) {
        this.placeId = placeId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItineraryPlaceId that = (ItineraryPlaceId) o;
        return Objects.equals(itineraryId, that.itineraryId) && 
               Objects.equals(placeId, that.placeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(itineraryId, placeId);
    }
    
    @Override
    public String toString() {
        return "ItineraryPlaceId{" +
                "itineraryId=" + itineraryId +
                ", placeId=" + placeId +
                '}';
    }
}