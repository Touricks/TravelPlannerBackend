package org.laioffer.planner.model.interest;

public class AddInterestRequest {
    private String placeId;
    private boolean pinned = false;
    
    public AddInterestRequest() {}
    
    public AddInterestRequest(String placeId) {
        this.placeId = placeId;
    }
    
    public AddInterestRequest(String placeId, boolean pinned) {
        this.placeId = placeId;
        this.pinned = pinned;
    }
    
    public String getPlaceId() {
        return placeId;
    }
    
    public void setPlaceId(String placeId) {
        this.placeId = placeId;
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
                "placeId='" + placeId + '\'' +
                ", pinned=" + pinned +
                '}';
    }
}