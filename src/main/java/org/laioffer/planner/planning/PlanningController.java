package org.laioffer.planner.planning;

import org.laioffer.planner.entity.UserEntity;
import org.laioffer.planner.planning.model.planning.PlanItineraryRequest;
import org.laioffer.planner.planning.model.planning.PlanItineraryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/itineraries")
public class PlanningController {

    private static final Logger logger = LoggerFactory.getLogger(PlanningController.class);
    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/{itineraryId}/plan")
    public ResponseEntity<PlanItineraryResponse> planItinerary(
            @PathVariable UUID itineraryId,
            @RequestBody PlanItineraryRequest request,
            @AuthenticationPrincipal UserEntity user) {

        logger.info("Planning request for itinerary {} by user: {}", itineraryId, user.getEmail());

        // Check ownership
        if (!planningService.isItineraryOwnedByUser(itineraryId, user.getId())) {
            logger.warn("User {} attempted to plan itinerary {} they don't own", user.getEmail(), itineraryId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PlanItineraryResponse response = planningService.generatePlan(itineraryId, request);
        return ResponseEntity.ok(response);
    }
}
