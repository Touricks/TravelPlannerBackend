package org.laioffer.planner.planning;

import org.laioffer.planner.Recommendations.model.planning.PlanItineraryRequest;
import org.laioffer.planner.Recommendations.model.planning.PlanItineraryResponse;

import java.util.UUID;

public interface PlanningService {
    PlanItineraryResponse generatePlan(UUID itineraryId, PlanItineraryRequest request);
}
