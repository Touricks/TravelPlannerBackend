package org.laioffer.planner.planning;

import org.laioffer.planner.Recommendations.model.planning.PlanItineraryRequest;
import org.laioffer.planner.Recommendations.model.planning.PlanItineraryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/itineraries/{itineraryId}/plan")
    public ResponseEntity<PlanItineraryResponse> planItinerary(
            @PathVariable UUID itineraryId,
            @RequestBody PlanItineraryRequest request) {
        PlanItineraryResponse response = planningService.generatePlan(itineraryId, request);
        return ResponseEntity.ok(response);
    }
}
