package org.laioffer.planner.Planning;

import org.laioffer.planner.Recommendation.model.planning.PlanItineraryRequest;
import org.laioffer.planner.Recommendation.model.planning.PlanItineraryResponse;

import java.util.UUID;

public interface PlanningService {
    PlanItineraryResponse generatePlan(UUID itineraryId, PlanItineraryRequest request);
}
