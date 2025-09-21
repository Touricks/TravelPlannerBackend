package org.example.aitripplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aitripplanner.dto.CreateItineraryRequest;
import org.example.aitripplanner.dto.TravelMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ItineraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateItinerary() throws Exception {
        CreateItineraryRequest request = new CreateItineraryRequest(
                "Tokyo",
                OffsetDateTime.parse("2025-10-01T15:00:00+08:00"),
                OffsetDateTime.parse("2025-10-05T20:00:00+08:00"),
                TravelMode.DRIVING,
                500_00,
                LocalTime.parse("09:00"),
                LocalTime.parse("20:00")
        );

        mockMvc.perform(post("/v1/itineraries")
                        .header("Authorization", "Bearer user-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itineraryId").isNotEmpty())
                .andExpect(jsonPath("$.destinationCity").value("Tokyo"))
                .andExpect(jsonPath("$.travelMode").value("DRIVING"))
                .andExpect(jsonPath("$.seededRecommendations").isArray())
                .andExpect(jsonPath("$.seededRecommendations[0].name").value("Tokyo City Highlights Tour"))
                .andExpect(jsonPath("$.seededRecommendations[0].address").value("Tokyo downtown"));
    }

    @Test
    void shouldRejectWhenAuthorizationMissing() throws Exception {
        CreateItineraryRequest request = new CreateItineraryRequest(
                "Paris",
                OffsetDateTime.parse("2025-05-01T09:00:00+02:00"),
                OffsetDateTime.parse("2025-05-03T09:00:00+02:00"),
                TravelMode.WALKING,
                null,
                null,
                null
        );

        mockMvc.perform(post("/v1/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
