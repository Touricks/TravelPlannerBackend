package org.laioffer.planner.planning;

import org.laioffer.planner.model.planning.PlanItineraryRequest;
import org.laioffer.planner.model.planning.PlanItineraryResponse;

import java.util.UUID;

public interface PlanningService {
    PlanItineraryResponse generatePlan(UUID itineraryId, PlanItineraryRequest request);

    boolean isItineraryOwnedByUser(UUID itineraryId, Long userId);
}
