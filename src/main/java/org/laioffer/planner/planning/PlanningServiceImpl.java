package org.laioffer.planner.planning;

import org.laioffer.planner.planning.model.planning.PlanItineraryRequest;
import org.laioffer.planner.planning.model.planning.PlanItineraryResponse;
import org.laioffer.planner.planning.model.planning.PlannedDay;
import org.laioffer.planner.planning.model.planning.PlannedStop;
import org.laioffer.planner.entity.ItineraryEntity;
import org.laioffer.planner.entity.ItineraryPlaceEntity;
import org.laioffer.planner.planning.ai.model.*;
import org.laioffer.planner.planning.exception.ItineraryNotFoundException;
import org.laioffer.planner.repository.ItineraryPlaceRepository;
import org.laioffer.planner.repository.ItineraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanningServiceImpl implements PlanningService {

    private static final Logger logger = LoggerFactory.getLogger(PlanningServiceImpl.class);

    private final ItineraryRepository itineraryRepository;
    private final ItineraryPlaceRepository itineraryPlaceRepository;
    private final PlanningLLMService planningLLMService;
    private final org.laioffer.planner.Recommendation.PlaceMapper placeMapper;

    public PlanningServiceImpl(
            ItineraryRepository itineraryRepository,
            ItineraryPlaceRepository itineraryPlaceRepository,
            PlanningLLMService planningLLMService,
            org.laioffer.planner.Recommendation.PlaceMapper placeMapper) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryPlaceRepository = itineraryPlaceRepository;
        this.planningLLMService = planningLLMService;
        this.placeMapper = placeMapper;
    }

    @Override
    public boolean isItineraryOwnedByUser(UUID itineraryId, Long userId) {
        return itineraryRepository.existsByIdAndUserId(itineraryId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanItineraryResponse generatePlan(UUID itineraryId, PlanItineraryRequest request) {
        // 1. Fetch the core itinerary information from the database.
        // If not found, this will throw an ItineraryNotFoundException.
        ItineraryEntity itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ItineraryNotFoundException("Itinerary with id " + itineraryId + " not found."));

        logger.info("Generating plan for itinerary {} in {}", itineraryId, itinerary.getDestinationCity());

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

        logger.debug("Found {} interested places for planning", interestedPlaces.size());

        // 3. Convert places to AI format
        List<AiPlaceInfo> aiPlaces = interestedPlaces.stream()
                .map(this::convertToAiPlaceInfo)
                .collect(Collectors.toList());

        logger.debug("Converted {} places to AI format. First place note value: '{}'",
                aiPlaces.size(), aiPlaces.isEmpty() ? "N/A" : aiPlaces.get(0).getNote());

        // 4. Prepare parameters for AI service
        LocalDate startDate = itinerary.getStartDate().toLocalDate();
        LocalDate endDate = itinerary.getEndDate().toLocalDate();
        String travelMode = itinerary.getTravelMode() != null ? itinerary.getTravelMode().name() : "WALKING";
        Integer budgetInCents = itinerary.getBudgetInCents() != null ? itinerary.getBudgetInCents() : 0;
        Double budgetInDollars = budgetInCents / 100.0;

        // Parse daily start/end times from request, or use defaults
        LocalTime dailyStart = request.getDailyStart() != null ? LocalTime.parse(request.getDailyStart()) : LocalTime.of(9, 0);
        LocalTime dailyEnd = request.getDailyEnd() != null ? LocalTime.parse(request.getDailyEnd()) : LocalTime.of(20, 0);

        // 5. Call LangChain4j AI service with retry logic
        AiPlanResponse aiResponse;
        try {
            aiResponse = planningLLMService.generatePlan(
                    itineraryId,
                    itinerary.getDestinationCity(),
                    startDate,
                    endDate,
                    travelMode,
                    budgetInCents,
                    budgetInDollars,
                    dailyStart,
                    dailyEnd,
                    aiPlaces
            );
        } catch (Exception e) {
            logger.error("Failed to generate plan for itinerary {}: {}", itineraryId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate travel plan: " + e.getMessage(), e);
        }

        // 6. Process the AI response and format it into PlanItineraryResponse
        return buildPlanResponse(itineraryId, aiResponse);
    }

    /**
     * Converts an ItineraryPlaceEntity to AiPlaceInfo for the AI request.
     */
    private AiPlaceInfo convertToAiPlaceInfo(ItineraryPlaceEntity itineraryPlace) {
        AiPlaceInfo aiPlace = new AiPlaceInfo();

        aiPlace.setPlaceId(itineraryPlace.getPlaceId());
        aiPlace.setName(itineraryPlace.getName() != null ? itineraryPlace.getName() : "");
        aiPlace.setPinned(itineraryPlace.isPinned());
        aiPlace.setNote(itineraryPlace.getNote() != null ? itineraryPlace.getNote() : "");

        // Get place details if available - ensure all fields are non-null for Mustache template
        if (itineraryPlace.getPlace() != null) {
            aiPlace.setAddress(itineraryPlace.getPlace().getAddress() != null ? itineraryPlace.getPlace().getAddress() : "");
            aiPlace.setLatitude(itineraryPlace.getPlace().getLatitude());
            aiPlace.setLongitude(itineraryPlace.getPlace().getLongitude());
            aiPlace.setDescription(itineraryPlace.getPlace().getDescription() != null ? itineraryPlace.getPlace().getDescription() : "");
        } else {
            aiPlace.setAddress("");
            aiPlace.setDescription("");
        }

        return aiPlace;
    }

    /**
     * Converts the AI response into the API response format.
     */
    private PlanItineraryResponse buildPlanResponse(UUID itineraryId, AiPlanResponse aiResponse) {
        PlanItineraryResponse response = new PlanItineraryResponse();

        // Set the itinerary ID
        response.setItineraryId(itineraryId);

        // Convert AI days to API format
        List<PlannedDay> days = aiResponse.getDays().stream()
                .map(aiDay -> convertToPlannedDay(itineraryId, aiDay))
                .collect(Collectors.toList());
        response.setDays(days);

        return response;
    }

    /**
     * Converts an AI planned day to the API PlannedDay format.
     */
    private PlannedDay convertToPlannedDay(UUID itineraryId, AiPlannedDay aiDay) {
        PlannedDay day = new PlannedDay();

        // Convert date to String format
        day.setDate(aiDay.getDate() != null ? aiDay.getDate().toString() : null);

        // Convert stops with order
        List<PlannedStop> stops = new java.util.ArrayList<>();
        for (int i = 0; i < aiDay.getStops().size(); i++) {
            stops.add(convertToPlannedStop(itineraryId, aiDay.getStops().get(i), i + 1));
        }
        day.setStops(stops);

        return day;
    }

    /**
     * Converts an AI planned stop to the API PlannedStop format.
     */
    private PlannedStop convertToPlannedStop(UUID itineraryId, AiPlannedStop aiStop, int order) {
        PlannedStop stop = new PlannedStop();

        // Set order
        stop.setOrder(order);

        // Set timing information
        stop.setArrivalLocal(aiStop.getArrivalTime() != null ? aiStop.getArrivalTime().toString() : null);
        stop.setDepartLocal(aiStop.getDepartureTime() != null ? aiStop.getDepartureTime().toString() : null);
        stop.setStayMinutes(aiStop.getDurationMinutes() != null ? aiStop.getDurationMinutes() : 0);
        stop.setNote(aiStop.getActivity());

        // Fetch and set place information if placeId is available
        if (aiStop.getPlaceId() != null) {
            itineraryPlaceRepository.findByItineraryIdAndPlaceId(itineraryId, aiStop.getPlaceId()).ifPresent(itineraryPlace -> {
                // Use PlaceMapper to convert ItineraryPlaceEntity to PlaceDTO
                org.laioffer.planner.model.place.PlaceDTO placeDTO = placeMapper.toItineraryPlaceDTO(itineraryPlace);
                stop.setPlace(placeDTO);
            });
        }

        return stop;
    }
}
