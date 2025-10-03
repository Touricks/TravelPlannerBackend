package org.laioffer.planner.itinerary;

import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.laioffer.planner.itinerary.model.llm.POIRecommendationResponse;

@AiService(chatModel = "openAiChatModel")
public interface POIRecommendationService {
    
    @SystemMessage("""
        You are a travel recommendation assistant that generates accurate point-of-interest (POI) recommendations for travelers.
        
        Your responsibilities:
        1. Generate authentic, popular tourist attractions and points of interest
        2. Provide accurate geographic coordinates (latitude/longitude)
        3. Include complete address information
        4. Write engaging but concise descriptions
        5. Include contact information when available
        
        Quality Requirements:
        - All coordinates MUST be accurate for the actual location
        - All required fields (name, address, description, location) are mandatory
        - Descriptions should be informative and engaging (50-150 words)
        - Verify that coordinates are within valid ranges (lat: -90 to 90, lng: -180 to 180)
        - Focus on well-known, accessible attractions suitable for tourists
        
        Response Format:
        - Return a structured POIRecommendationResponse object
        - Include the exact number of recommendations requested
        - Ensure all POI objects have complete required information
        """)
    @UserMessage("""
        Generate {{maxRecommendations}} popular tourist attractions and points of interest for {{destinationCity}}.

        Trip Details:
        - Destination: {{destinationCity}}
        - Number of recommendations needed: {{maxRecommendations}}
        {{#budgetInCents}}- Budget consideration: ${{budgetInDollars}}{{/budgetInCents}}
        {{#travelMode}}- Travel mode: {{travelMode}}{{/travelMode}}
        {{#stayingDays}}- Trip duration: {{stayingDays}} days{{/stayingDays}}

        Traveler Profile:
        {{#travelPace}}- Travel pace: {{travelPace}} (adjust POI density accordingly){{/travelPace}}
        {{#activityIntensity}}- Activity intensity preference: {{activityIntensity}}{{/activityIntensity}}
        {{#numberOfTravelers}}- Group size: {{numberOfTravelers}} travelers{{/numberOfTravelers}}
        {{#hasChildren}}- Traveling with children: yes (prioritize family-friendly attractions){{/hasChildren}}
        {{#hasElderly}}- Traveling with elderly: yes (prioritize accessible, low-intensity activities){{/hasElderly}}
        {{#preferPopularAttractions}}- Preference: Well-known, popular tourist attractions{{/preferPopularAttractions}}
        {{^preferPopularAttractions}}- Preference: Off-the-beaten-path, unique experiences{{/preferPopularAttractions}}
        {{#preferredCategories}}
        - Preferred attraction types: {{#.}}{{.}}, {{/.}}
        {{/preferredCategories}}
        {{#additionalPreferences}}
        - Additional preferences: {{additionalPreferences}}
        {{/additionalPreferences}}

        Focus on attractions that are:
        - Suitable for the specified travel mode, pace, and activity level
        - Appropriate for the budget level and group composition
        - Matching the preferred categories when specified
        - Accessible and open to visitors

        Ensure each recommendation includes:
        - Accurate name and full street address
        - Precise latitude/longitude coordinates
        - Compelling description highlighting key features
        - Contact information (website, phone) when available
        - Opening hours information when relevant
        """)
    POIRecommendationResponse generatePOIRecommendations(
            @V("destinationCity") String destinationCity,
            @V("maxRecommendations") int maxRecommendations,
            @V("budgetInCents") Integer budgetInCents,
            @V("budgetInDollars") Double budgetInDollars,
            @V("travelMode") String travelMode,
            @V("stayingDays") Integer stayingDays,
            @V("travelPace") String travelPace,
            @V("activityIntensity") String activityIntensity,
            @V("numberOfTravelers") Integer numberOfTravelers,
            @V("hasChildren") Boolean hasChildren,
            @V("hasElderly") Boolean hasElderly,
            @V("preferPopularAttractions") Boolean preferPopularAttractions,
            @V("preferredCategories") java.util.List<String> preferredCategories,
            @V("additionalPreferences") String additionalPreferences
    );
    
    @SystemMessage("""
        You are a travel recommendation assistant. The previous attempt to generate POI recommendations failed.
        Please learn from the errors provided and generate better recommendations.
        
        Common issues to avoid:
        - Invalid JSON structure
        - Missing required fields (name, address, description, location)
        - Invalid coordinate ranges
        - Empty or null values for required fields
        - Incorrect data types
        
        Focus on providing complete, accurate, and well-structured recommendations.
        """)
    @UserMessage("""
        Generate {{maxRecommendations}} popular tourist attractions for {{destinationCity}}.
        
        PREVIOUS ERRORS TO FIX:
        {{#errorHistory}}
        - {{.}}
        {{/errorHistory}}
        
        Trip Details:
        - Destination: {{destinationCity}}
        - Number of recommendations: {{maxRecommendations}}
        {{#budgetInCents}}- Budget: ${{budgetInDollars}}{{/budgetInCents}}
        {{#travelMode}}- Travel mode: {{travelMode}}{{/travelMode}}
        
        CRITICAL: Fix the errors mentioned above and ensure:
        - All required fields are present and non-empty
        - Coordinates are valid (lat: -90 to 90, lng: -180 to 180)
        - Proper data types are used
        - Complete address information is provided
        """)
    POIRecommendationResponse generatePOIRecommendationsWithErrorFeedback(
            @V("destinationCity") String destinationCity,
            @V("maxRecommendations") int maxRecommendations,
            @V("budgetInCents") Integer budgetInCents,
            @V("budgetInDollars") Double budgetInDollars,
            @V("travelMode") String travelMode,
            @V("errorHistory") java.util.List<String> errorHistory
    );
}