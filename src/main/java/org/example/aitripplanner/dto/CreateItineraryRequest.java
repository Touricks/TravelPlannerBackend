package org.example.aitripplanner.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.OffsetDateTime;

public record CreateItineraryRequest(
        @NotBlank String destinationCity,
        @NotNull OffsetDateTime startDate,
        @NotNull OffsetDateTime endDate,
        TravelMode travelMode,
        Integer budgetLimitCents,
        @JsonFormat(pattern = "HH:mm") LocalTime dailyStart,
        @JsonFormat(pattern = "HH:mm") LocalTime dailyEnd
) {
}
