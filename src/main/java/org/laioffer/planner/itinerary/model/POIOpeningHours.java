package org.laioffer.planner.itinerary.model;

import dev.langchain4j.model.output.structured.Description;

public class POIOpeningHours {
    
    @Description("Raw opening hours text (e.g., 'Mon-Sun 9:00-18:00')")
    private String raw;
    
    public POIOpeningHours() {}
    
    public POIOpeningHours(String raw) {
        this.raw = raw;
    }
    
    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }
}