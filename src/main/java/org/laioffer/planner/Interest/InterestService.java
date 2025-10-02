package org.laioffer.planner.Interest;

import org.laioffer.planner.entity.UserEntity;

public interface InterestService {
    
    /**
     * Add a place to the itinerary's interest list
     * 
     * @param request AddInterestRequest containing Itinerary_placeId and pinned status
     * @param user Authenticated user
     * @return AddInterestResponse with place details and pinned status
     * @throws IllegalArgumentException if Itinerary_placeId format is invalid
     * @throws SecurityException if user doesn't own the itinerary
     * @throws RuntimeException if itinerary or place not found
     */
    AddInterestResponse addInterest(AddInterestRequest request, UserEntity user);
}