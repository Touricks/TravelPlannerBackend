package org.laioffer.planner.itinerary;

import org.laioffer.planner.Recommendations.model.itinerary.CreateItineraryRequest;
import org.laioffer.planner.entity.ItineraryEntity;
import org.laioffer.planner.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface ItineraryService {
    
    /**
     * Creates a new itinerary with AI-generated POI recommendations
     * 
     * @param request The itinerary creation request containing trip details
     * @param user The authenticated user creating the itinerary
     * @return The UUID of the created itinerary
     * @throws IllegalArgumentException if request validation fails
     */
    UUID createItinerary(CreateItineraryRequest request, UserEntity user);
    
    /**
     * Retrieves an itinerary by its ID
     * 
     * @param itineraryId The UUID of the itinerary
     * @return Optional containing the itinerary if found
     */
    Optional<ItineraryEntity> getItinerary(UUID itineraryId);
    
    /**
     * Checks if an itinerary belongs to a specific user
     * 
     * @param itineraryId The UUID of the itinerary
     * @param userId The ID of the user
     * @return true if the itinerary belongs to the user, false otherwise
     */
    boolean isItineraryOwnedByUser(UUID itineraryId, Long userId);
}