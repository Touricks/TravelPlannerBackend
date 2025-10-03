package org.laioffer.planner.Planning;

import org.laioffer.planner.Planning.model.planning.PlanItineraryRequest;
import org.laioffer.planner.Planning.model.planning.PlanItineraryResponse;
import org.laioffer.planner.entity.ItineraryEntity;
import org.laioffer.planner.entity.ItineraryPlaceEntity;
import org.laioffer.planner.Planning.exception.ItineraryNotFoundException;
import org.laioffer.planner.repository.ItineraryPlaceRepository;
import org.laioffer.planner.repository.ItineraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;

@Service
public class PlanningServiceImpl implements PlanningService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryPlaceRepository itineraryPlaceRepository;

    public PlanningServiceImpl(ItineraryRepository itineraryRepository, ItineraryPlaceRepository itineraryPlaceRepository) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryPlaceRepository = itineraryPlaceRepository;
    }

    @Override
    public PlanItineraryResponse generatePlan(UUID itineraryId, PlanItineraryRequest request) {
        // 1. Fetch the core itinerary information from the database.
        // If not found, this will throw an ItineraryNotFoundException.
        ItineraryEntity itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ItineraryNotFoundException("Itinerary with id " + itineraryId + " not found."));

        // 2. Fetch the list of places the user is interested in.
        // If the request specifies a list of places, use that. Otherwise, use all
        // places previously added to the itinerary's interest list.
        List<ItineraryPlaceEntity> interestedPlaces;
        if (CollectionUtils.isEmpty(request.getInterestPlaceIds())) {
            interestedPlaces = itineraryPlaceRepository.findAllByItineraryId(itineraryId);
        } else {
            List<UUID> placeIds = request.getInterestPlaceIds().stream().map(UUID::fromString).toList();
            interestedPlaces = itineraryPlaceRepository.findAllByItineraryIdAndIdIn(itineraryId, placeIds);
        }

        // TODO: 3. Prepare the request for the AI model (MCP).
        // This will involve combining itinerary constraints (dates, budget, etc.)
        // with the list of interested places.

        // TODO: 4. Call the AI model to get the optimized plan.

        // TODO: 5. Process the AI response and format it into PlanItineraryResponse.

        return new PlanItineraryResponse(); // Placeholder
    }
}
