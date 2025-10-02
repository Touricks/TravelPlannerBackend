package org.laioffer.planner.Interest;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AddInterestRequest {
    @NotNull(message = "itineraryPlaceId is required")
    private UUID itineraryPlaceId;

    private boolean pinned = true;

    public AddInterestRequest() {}

    public AddInterestRequest(UUID itineraryPlaceId) {
        this.itineraryPlaceId = itineraryPlaceId;
    }

    public AddInterestRequest(UUID itineraryPlaceId, boolean pinned) {
        this.itineraryPlaceId = itineraryPlaceId;
        this.pinned = pinned;
    }

    public UUID getItineraryPlaceId() {
        return itineraryPlaceId;
    }

    public void setItineraryPlaceId(UUID itineraryPlaceId) {
        this.itineraryPlaceId = itineraryPlaceId;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    @Override
    public String toString() {
        return "AddInterestRequest{" +
                "itineraryPlaceId=" + itineraryPlaceId +
                ", pinned=" + pinned +
                '}';
    }
}