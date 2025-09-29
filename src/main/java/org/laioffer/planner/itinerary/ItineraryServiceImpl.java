package org.laioffer.planner.itinerary;

import org.laioffer.planner.Recommendations.model.itinerary.CreateItineraryRequest;
import org.laioffer.planner.entity.ItineraryEntity;
import org.laioffer.planner.entity.UserEntity;
import org.laioffer.planner.repository.ItineraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ItineraryServiceImpl implements ItineraryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryServiceImpl.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_POI_COUNT = 15;
    private static final int POI_PER_DAY = 3;
    
    private final ItineraryRepository itineraryRepository;
    
    @Autowired
    public ItineraryServiceImpl(ItineraryRepository itineraryRepository) {
        this.itineraryRepository = itineraryRepository;
    }
    
    @Override
    public UUID createItinerary(CreateItineraryRequest request, UserEntity user) {
        logger.debug("Creating itinerary for user: {}", user.getId());
        
        validateRequest(request);
        
        int stayingDays = calculateStayingDays(request);
        int poiCount = calculatePOICount(stayingDays);
        
        logger.info("Itinerary details - Destination: {}, Days: {}, POI Count: {}", 
                request.getDestinationCity(), stayingDays, poiCount);
        
        ItineraryEntity itinerary = new ItineraryEntity();
        itinerary.setUser(user);
        itinerary.setDestinationCity(request.getDestinationCity());
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setTravelMode(request.getTravelMode());
        itinerary.setBudgetInCents(request.getBudgetLimitCents());
        
        if (request.getDailyStart() != null) {
            itinerary.setDailyStart(parseTime(request.getDailyStart()));
        }
        if (request.getDailyEnd() != null) {
            itinerary.setDailyEnd(parseTime(request.getDailyEnd()));
        }
        
        Map<String, Object> aiMetadata = new HashMap<>();
        aiMetadata.put("staying_days", stayingDays);
        aiMetadata.put("recommended_poi_count", poiCount);
        aiMetadata.put("generation_pending", true);
        itinerary.setAiMetadata(aiMetadata);
        
        ItineraryEntity savedItinerary = itineraryRepository.save(itinerary);
        
        logger.info("Successfully created itinerary: {} for user: {}", 
                savedItinerary.getId(), user.getId());
        
        return savedItinerary.getId();
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
        if (request.getDestinationCity() == null || request.getDestinationCity().trim().isEmpty()) {
            throw new IllegalArgumentException("Destination city is required");
        }
        
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        
        if (request.getBudgetLimitCents() != null && request.getBudgetLimitCents() < 0) {
            throw new IllegalArgumentException("Budget cannot be negative");
        }
        
        if (request.getDailyStart() != null && request.getDailyEnd() != null) {
            LocalTime startTime = parseTime(request.getDailyStart());
            LocalTime endTime = parseTime(request.getDailyEnd());
            if (startTime.isAfter(endTime)) {
                throw new IllegalArgumentException("Daily start time must be before end time");
            }
        }
    }
    
    private int calculateStayingDays(CreateItineraryRequest request) {
        long days = ChronoUnit.DAYS.between(
                request.getStartDate().toLocalDate(), 
                request.getEndDate().toLocalDate()
        );
        
        if (days == 0) {
            days = 1;
        }
        
        return (int) days;
    }
    
    private int calculatePOICount(int stayingDays) {
        return Math.min(stayingDays * POI_PER_DAY, MAX_POI_COUNT);
    }
    
    private LocalTime parseTime(String timeString) {
        try {
            return LocalTime.parse(timeString, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Expected HH:mm, got: " + timeString);
        }
    }
}