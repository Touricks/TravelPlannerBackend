package org.laioffer.planner.itinerary;

import org.laioffer.planner.Recommendations.model.itinerary.CreateItineraryRequest;
import org.laioffer.planner.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/itineraries")
@CrossOrigin(origins = "*")
public class ItineraryController {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryController.class);
    
    private final ItineraryService itineraryService;
    
    @Autowired
    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }
    
    /**
     * Creates a new itinerary with AI-generated place recommendations
     * 
     * @param request CreateItineraryRequest containing trip details
     * @param user Authenticated user from JWT token
     * @return HTTP 201 Created with Location header, no response body
     */
    @PostMapping
    public ResponseEntity<Void> createItinerary(
            @Validated @RequestBody CreateItineraryRequest request,
            @AuthenticationPrincipal UserEntity user) {
        
        logger.info("Creating itinerary for user: {} to destination: {}", 
                user.getEmail(), request.getDestinationCity());
        
        try {
            UUID itineraryId = itineraryService.createItinerary(request, user);
            
            URI location = URI.create("/api/itineraries/" + itineraryId);
            
            logger.info("Successfully created itinerary: {} for user: {}", 
                    itineraryId, user.getEmail());
            
            return ResponseEntity.created(location).build();
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for user {}: {}", user.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Error creating itinerary for user {}: {}", 
                    user.getEmail(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}