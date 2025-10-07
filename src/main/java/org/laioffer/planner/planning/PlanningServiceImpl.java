package org.laioffer.planner.planning;

import org.laioffer.planner.Recommendations.model.planning.PlanItineraryRequest;
import org.laioffer.planner.Recommendations.model.planning.PlanItineraryResponse;
import org.laioffer.planner.Recommendations.model.planning.PlannedDay;
import org.laioffer.planner.Recommendations.model.planning.PlannedStop;
import org.laioffer.planner.entity.ItineraryEntity;
import org.laioffer.planner.entity.ItineraryPlaceEntity;
import org.laioffer.planner.planning.ai.McpAiClient;
import org.laioffer.planner.planning.ai.model.*;
import org.laioffer.planner.planning.exception.ItineraryNotFoundException;
import org.laioffer.planner.repository.ItineraryPlaceRepository;
import org.laioffer.planner.repository.ItineraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanningServiceImpl implements PlanningService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryPlaceRepository itineraryPlaceRepository;
    private final McpAiClient mcpAiClient;

    public PlanningServiceImpl(
            ItineraryRepository itineraryRepository,
            ItineraryPlaceRepository itineraryPlaceRepository,
            McpAiClient mcpAiClient) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryPlaceRepository = itineraryPlaceRepository;
        this.mcpAiClient = mcpAiClient;
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

        // 3. Prepare the request for the AI model (MCP)
        AiPlanRequest aiRequest = buildAiRequest(itinerary, interestedPlaces);

        // 4. Call the AI model to get the optimized plan
        AiPlanResponse aiResponse = mcpAiClient.generatePlan(aiRequest);

        // 5. Process the AI response and format it into PlanItineraryResponse
        return buildPlanResponse(aiResponse);
    }

    /**
     * Builds the AI request by combining itinerary information and interested places.
     */
    private AiPlanRequest buildAiRequest(ItineraryEntity itinerary, List<ItineraryPlaceEntity> interestedPlaces) {
        AiPlanRequest aiRequest = new AiPlanRequest();

        // Set itinerary constraints
        aiRequest.setDestinationCity(itinerary.getDestinationCity());
        aiRequest.setStartDate(itinerary.getStartDate());
        aiRequest.setEndDate(itinerary.getEndDate());
        aiRequest.setTravelMode(itinerary.getTravelMode() != null ? itinerary.getTravelMode().name() : null);
        aiRequest.setBudgetInCents(itinerary.getBudgetInCents());
        aiRequest.setDailyStart(itinerary.getDailyStart());
        aiRequest.setDailyEnd(itinerary.getDailyEnd());

        // Convert interested places to AI format
        List<AiPlaceInfo> aiPlaces = interestedPlaces.stream()
                .map(this::convertToAiPlaceInfo)
                .collect(Collectors.toList());
        aiRequest.setInterestedPlaces(aiPlaces);

        return aiRequest;
    }

    /**
     * Converts an ItineraryPlaceEntity to AiPlaceInfo for the AI request.
     */
    private AiPlaceInfo convertToAiPlaceInfo(ItineraryPlaceEntity itineraryPlace) {
        AiPlaceInfo aiPlace = new AiPlaceInfo();

        aiPlace.setPlaceId(itineraryPlace.getPlaceId());
        aiPlace.setName(itineraryPlace.getName());
        aiPlace.setPinned(itineraryPlace.isPinned());
        aiPlace.setNote(itineraryPlace.getNote());

        // Get place details if available
        if (itineraryPlace.getPlace() != null) {
            aiPlace.setAddress(itineraryPlace.getPlace().getAddress());
            aiPlace.setLatitude(itineraryPlace.getPlace().getLatitude());
            aiPlace.setLongitude(itineraryPlace.getPlace().getLongitude());
            aiPlace.setDescription(itineraryPlace.getPlace().getDescription());
        }

        return aiPlace;
    }

    /**
     * Converts the AI response into the API response format.
     */
    private PlanItineraryResponse buildPlanResponse(AiPlanResponse aiResponse) {
        PlanItineraryResponse response = new PlanItineraryResponse();

        // Convert AI days to API format
        List<PlannedDay> days = aiResponse.getDays().stream()
                .map(this::convertToPlannedDay)
                .collect(Collectors.toList());
        response.setDays(days);

        return response;
    }

    /**
     * Converts an AI planned day to the API PlannedDay format.
     */
    private PlannedDay convertToPlannedDay(AiPlannedDay aiDay) {
        PlannedDay day = new PlannedDay();

        // Convert date to String format
        day.setDate(aiDay.getDate() != null ? aiDay.getDate().toString() : null);

        // Convert stops
        List<PlannedStop> stops = aiDay.getStops().stream()
                .map(this::convertToPlannedStop)
                .collect(Collectors.toList());
        day.setStops(stops);

        return day;
    }

    /**
     * Converts an AI planned stop to the API PlannedStop format.
     */
    private PlannedStop convertToPlannedStop(AiPlannedStop aiStop) {
        PlannedStop stop = new PlannedStop();

        // Note: PlannedStop expects PlaceDTO, but for now we'll set basic fields
        // You may need to fetch full PlaceDTO from database if needed
        stop.setArrivalLocal(aiStop.getArrivalTime() != null ? aiStop.getArrivalTime().toString() : null);
        stop.setDepartLocal(aiStop.getDepartureTime() != null ? aiStop.getDepartureTime().toString() : null);
        stop.setStayMinutes(aiStop.getDurationMinutes() != null ? aiStop.getDurationMinutes() : 0);
        stop.setNote(aiStop.getActivity());

        return stop;
    }
}
