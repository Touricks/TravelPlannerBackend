package org.example.aitripplanner.service;

import org.example.aitripplanner.dto.CreateItineraryRequest;
import org.example.aitripplanner.dto.CreateItineraryResponse;
import org.example.aitripplanner.dto.PlaceDto;
import org.example.aitripplanner.dto.TravelMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ItineraryService {

    private final Map<UUID, StoredItinerary> store = new ConcurrentHashMap<>();
    private final ItineraryGenerator itineraryGenerator;

    public ItineraryService(ItineraryGenerator itineraryGenerator) {
        this.itineraryGenerator = itineraryGenerator;
    }

    public CreateItineraryResponse createItinerary(String userId, CreateItineraryRequest request) {
        validateRequest(request);

        UUID itineraryId = UUID.randomUUID();
        StoredItinerary itinerary = new StoredItinerary(
                itineraryId,
                userId,
                request.destinationCity(),
                request.startDate(),
                request.endDate(),
                request.travelMode(),
                request.budgetLimitCents(),
                request.dailyStart(),
                request.dailyEnd()
        );

        store.put(itineraryId, itinerary);

        List<PlaceDto> recommendations = itineraryGenerator.generatePlaces(request);

        return new CreateItineraryResponse(
                itineraryId,
                itinerary.destinationCity(),
                itinerary.startDate(),
                itinerary.endDate(),
                itinerary.travelMode(),
                itinerary.budgetLimitCents(),
                itinerary.dailyStart(),
                itinerary.dailyEnd(),
                recommendations
        );
    }

    private void validateRequest(CreateItineraryRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }

        if (request.budgetLimitCents() != null && request.budgetLimitCents() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "budgetLimitCents must be zero or positive");
        }

        LocalTime dailyStart = request.dailyStart();
        LocalTime dailyEnd = request.dailyEnd();
        if (dailyStart != null && dailyEnd != null && dailyStart.isAfter(dailyEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dailyStart must be before or equal to dailyEnd");
        }
    }

    private record StoredItinerary(
            UUID itineraryId,
            String userId,
            String destinationCity,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            TravelMode travelMode,
            Integer budgetLimitCents,
            LocalTime dailyStart,
            LocalTime dailyEnd
    ) {
    }
}
