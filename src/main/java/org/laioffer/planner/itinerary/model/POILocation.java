package org.laioffer.planner.itinerary.model;

import dev.langchain4j.model.output.structured.Description;

public class POILocation {
    
    @Description("Latitude coordinate (between -90 and 90)")
    private double lat;
    
    @Description("Longitude coordinate (between -180 and 180)")
    private double lng;
    
    public POILocation() {}
    
    public POILocation(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }
    
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
}