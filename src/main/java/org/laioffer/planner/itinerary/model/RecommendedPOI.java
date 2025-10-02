package org.laioffer.planner.itinerary.model;

import dev.langchain4j.model.output.structured.Description;

public class RecommendedPOI {
    
    @Description("Name of the tourist attraction or point of interest")
    private String name;
    
    @Description("Full street address of the location")
    private String address;
    
    @Description("Brief description of the attraction (required)")
    private String description;
    
    @Description("Geographic location with latitude and longitude coordinates")
    private POILocation location;
    
    @Description("Contact information including website and phone (optional)")
    private POIContact contact;
    
    @Description("URL to an image of the attraction (optional)")
    private String imageUrl;
    
    @Description("Opening hours information (optional)")
    private POIOpeningHours openingHours;
    
    public RecommendedPOI() {}
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public POILocation getLocation() { return location; }
    public void setLocation(POILocation location) { this.location = location; }
    
    public POIContact getContact() { return contact; }
    public void setContact(POIContact contact) { this.contact = contact; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public POIOpeningHours getOpeningHours() { return openingHours; }
    public void setOpeningHours(POIOpeningHours openingHours) { this.openingHours = openingHours; }
}