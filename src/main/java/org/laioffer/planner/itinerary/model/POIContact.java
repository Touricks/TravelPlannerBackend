package org.laioffer.planner.itinerary.model;

import dev.langchain4j.model.output.structured.Description;

public class POIContact {
    
    @Description("Website URL (optional)")
    private String website;
    
    @Description("Phone number (optional)")
    private String phone;
    
    public POIContact() {}
    
    public POIContact(String website, String phone) {
        this.website = website;
        this.phone = phone;
    }
    
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}