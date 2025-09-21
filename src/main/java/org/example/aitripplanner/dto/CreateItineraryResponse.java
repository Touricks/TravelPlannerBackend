package org.example.aitripplanner.dto;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateItineraryResponse(
        UUID itineraryId,
        String destinationCity,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        TravelMode travelMode,
        Integer budgetLimitCents,
        LocalTime dailyStart,
        LocalTime dailyEnd,
        List<PlaceDto> seededRecommendations
) {
}
