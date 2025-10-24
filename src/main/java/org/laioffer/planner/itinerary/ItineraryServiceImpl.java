package org.laioffer.planner.itinerary;

import org.laioffer.planner.model.itinerary.CreateItineraryRequest;
import org.laioffer.planner.model.place.PlaceDTO;
import org.laioffer.planner.model.common.TravelPace;
import org.laioffer.planner.entity.ItineraryEntity;
import org.laioffer.planner.entity.PlaceEntity;
import org.laioffer.planner.entity.UserEntity;
import org.laioffer.planner.repository.ItineraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ItineraryServiceImpl implements ItineraryService {

    private static final Logger logger = LoggerFactory.getLogger(ItineraryServiceImpl.class);
    private static final int MAX_POI_COUNT = 16;
    private static final int MAX_STAYING_DAYS = 7;

    private final ItineraryRepository itineraryRepository;
    private final LangChain4jLLMService llmService;
    private final POIService poiService;
    private final ApplicationEventPublisher eventPublisher;

    public ItineraryServiceImpl(ItineraryRepository itineraryRepository,
                               LangChain4jLLMService llmService,
                               POIService poiService,
                               ApplicationEventPublisher eventPublisher) {
        this.itineraryRepository = itineraryRepository;
        this.llmService = llmService;
        this.poiService = poiService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ItineraryEntity createItinerary(CreateItineraryRequest request, UserEntity user) {
        logger.debug("Creating itinerary for user: {}", user.getId());

        validateRequest(request);

        int stayingDays = calculateStayingDays(request);
        int poiCount = calculatePOICount(stayingDays, request.getTravelPace());

        logger.info("Itinerary details - Destination: {}, Days: {}, POI Count: {}, Travel Pace: {}",
                request.getDestinationCity(), stayingDays, poiCount, request.getTravelPace());

        ItineraryEntity itinerary = new ItineraryEntity();
        itinerary.setUser(user);
        itinerary.setDestinationCity(request.getDestinationCity());
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setTravelMode(request.getTravelMode());
        itinerary.setBudgetInCents(request.getBudgetLimitCents());

        // Map user preference fields
        itinerary.setTravelPace(request.getTravelPace());
        itinerary.setActivityIntensity(request.getActivityIntensity());
        itinerary.setNumberOfTravelers(request.getNumberOfTravelers());
        itinerary.setHasChildren(request.getHasChildren());
        itinerary.setHasElderly(request.getHasElderly());
        itinerary.setPreferPopularAttractions(request.getPreferPopularAttractions());

        // Convert List<AttractionCategory> enum to List<String> for storage
        if (request.getPreferredCategories() != null && !request.getPreferredCategories().isEmpty()) {
            itinerary.setPreferredCategories(
                request.getPreferredCategories().stream()
                    .map(Enum::name)
                    .toList()
            );
        } else {
            itinerary.setPreferredCategories(new java.util.ArrayList<>());  // Ensure non-null
        }

        itinerary.setAdditionalPreferences(request.getAdditionalPreferences());

        Map<String, Object> aiMetadata = new HashMap<>();
        aiMetadata.put("staying_days", stayingDays);
        aiMetadata.put("recommended_poi_count", poiCount);
        aiMetadata.put("generation_pending", true);
        itinerary.setAiMetadata(aiMetadata);

        logger.info("🔍 DEBUG - Before saving to DB: travelPace={}, activityIntensity={}",
                itinerary.getTravelPace(), itinerary.getActivityIntensity());

        ItineraryEntity savedItinerary = itineraryRepository.save(itinerary);

        logger.info("🔍 DEBUG - After saving to DB: travelPace={}, activityIntensity={}",
                savedItinerary.getTravelPace(), savedItinerary.getActivityIntensity());

        logger.info("Successfully created itinerary: {} for user: {}",
                savedItinerary.getId(), user.getId());

        // Publish event to trigger async POI generation after transaction commits
        eventPublisher.publishEvent(new ItineraryCreatedEvent(savedItinerary.getId(), poiCount));

        return savedItinerary;
    }

    /**
     * Generates POI recommendations using LLM
     * Called from async event listener after transaction commits
     */
    @Transactional
    public void generatePlacesAsync(UUID itineraryId, int poiCount) {
        logger.info("Starting async POI generation for itinerary: {}", itineraryId);

        // Re-fetch the itinerary in this transaction context
        Optional<ItineraryEntity> itineraryOpt = itineraryRepository.findById(itineraryId);
        if (itineraryOpt.isEmpty()) {
            logger.error("Itinerary {} not found for async generation", itineraryId);
            return;
        }

        ItineraryEntity itinerary = itineraryOpt.get();
        Map<String, Object> aiMetadata = itinerary.getAiMetadata();
        if (aiMetadata == null) {
            aiMetadata = new HashMap<>();
        }

        try {
            List<PlaceDTO> recommendedPlaces = llmService.generatePOIRecommendations(itinerary, poiCount);
            List<PlaceEntity> createdPlaces = poiService.createAndAddPlacesToItinerary(recommendedPlaces, itinerary);

            aiMetadata.put("generation_pending", false);
            aiMetadata.put("generated_places_count", createdPlaces.size());
            itinerary.setAiMetadata(aiMetadata);

            itineraryRepository.save(itinerary);

            logger.info("Successfully generated {} POI recommendations for itinerary: {}",
                    createdPlaces.size(), itinerary.getId());

        } catch (Exception e) {
            logger.error("Failed to generate POI recommendations for itinerary: {}", itinerary.getId(), e);
            aiMetadata.put("generation_pending", false);
            aiMetadata.put("generation_error", e.getMessage());
            itinerary.setAiMetadata(aiMetadata);
            itineraryRepository.save(itinerary);
        }
    }

    @Override
    public Optional<ItineraryEntity> getItinerary(UUID itineraryId) {
        return itineraryRepository.findById(itineraryId);
    }

    @Override
    public boolean isItineraryOwnedByUser(UUID itineraryId, Long userId) {
        return itineraryRepository.existsByIdAndUserId(itineraryId, userId);
    }

    private void validateRequest(CreateItineraryRequest request) {
        // Validate required core fields
        if (request.getDestinationCity() == null || request.getDestinationCity().trim().isEmpty()) {
            throw new IllegalArgumentException("Destination city is required");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        // Apply defaults for optional fields instead of throwing exceptions
        if (request.getTravelPace() == null) {
            logger.info("TravelPace not provided, using default: MODERATE");
            request.setTravelPace(TravelPace.MODERATE);
        }

        if (request.getBudgetLimitCents() == null || request.getBudgetLimitCents() <= 0) {
            logger.info("Budget not provided or invalid, using default: 200000 cents ($2000)");
            request.setBudgetLimitCents(200000); // Default $2000
        }

        if (request.getNumberOfTravelers() == null || request.getNumberOfTravelers() <= 0) {
            logger.info("NumberOfTravelers not provided or invalid, using default: 1");
            request.setNumberOfTravelers(1);
        }

        // Log when defaults are applied
        logger.debug("Request validation complete. TravelPace: {}, Budget: {}, Travelers: {}",
                request.getTravelPace(), request.getBudgetLimitCents(), request.getNumberOfTravelers());
    }

    private int calculateStayingDays(CreateItineraryRequest request) {
        long days = ChronoUnit.DAYS.between(
                request.getStartDate().toLocalDate(),
                request.getEndDate().toLocalDate()
        );

        if (days == 0) {
            days = 1;
        }

        // Cap staying days to reduce POI generation load
        return (int) Math.min(days, MAX_STAYING_DAYS);
    }

    private int calculatePOICount(int stayingDays, TravelPace pace) {
        int poiPerDay = switch (pace) {
            case RELAXED -> 2;
            case MODERATE -> 4;
            case PACKED -> 5;
        };
        return Math.min(stayingDays * poiPerDay, MAX_POI_COUNT);
    }

    @Override
    public Page<ItineraryEntity> getUserItineraries(Long userId, Pageable pageable) {
        logger.debug("Fetching itineraries for user: {} with pagination: {}", userId, pageable);
        return itineraryRepository.findByUserId(userId, pageable);
    }

    @Override
    public Optional<ItineraryEntity> getItineraryWithPlaces(UUID itineraryId) {
        logger.debug("Fetching itinerary with places: {}", itineraryId);
        Optional<ItineraryEntity> itineraryOpt = itineraryRepository.findById(itineraryId);

        // Eagerly load the places to avoid lazy loading issues
        itineraryOpt.ifPresent(itinerary -> {
            itinerary.getItineraryPlaces().size(); // Force initialization
        });

        return itineraryOpt;
    }

    /**
     * Event listener that triggers async POI generation after transaction commits
     * This ensures the itinerary is persisted before async processing begins
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleItineraryCreated(ItineraryCreatedEvent event) {
        logger.info("Handling ItineraryCreatedEvent for itinerary: {}", event.getItineraryId());
        generatePlacesAsync(event.getItineraryId(), event.getPoiCount());
    }
}
