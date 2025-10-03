package org.laioffer.planner.itinerary;

import org.laioffer.planner.model.itinerary.CreateItineraryRequest;
import org.laioffer.planner.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryController.class);
    private final ItineraryService itineraryService;
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
            itineraryService.createItinerary(request, user);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating itinerary for user {}: {}", 
                    user.getEmail(), e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}