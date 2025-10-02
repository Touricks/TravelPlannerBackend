package org.laioffer.planner.itinerary;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.laioffer.planner.Recommendation.model.itinerary.CreateItineraryRequest;
import org.laioffer.planner.Recommendation.model.itinerary.TravelMode;
import org.laioffer.planner.entity.ItineraryEntity;
import org.laioffer.planner.repository.ItineraryRepository;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "langchain4j.openai.chat-model.api-key=test-key"
})
@AutoConfigureMockMvc
@Transactional
class ItineraryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItineraryRepository itineraryRepository;

    @MockBean
    private LLMService llmService;

    @MockBean
    private POIService poiService;

    @MockBean
    private POIRecommendationService poiRecommendationService;

    @Test
    @WithMockUser(username = "test@example.com")
    void testCreateItinerary_Success() throws Exception {
        // Given
        CreateItineraryRequest request = new CreateItineraryRequest();
        request.setDestinationCity("San Francisco");
        request.setStartDate(OffsetDateTime.parse("2024-03-01T10:00:00-08:00"));
        request.setEndDate(OffsetDateTime.parse("2024-03-05T18:00:00-08:00"));
        request.setTravelMode(TravelMode.DRIVING);
        request.setBudgetLimitCents(500000);
        request.setDailyStart("09:00");
        request.setDailyEnd("21:00");

        String requestJson = objectMapper.writeValueAsString(request);

        // Mock the external services to avoid external calls
        Mockito.when(llmService.generatePOIRecommendations(Mockito.any(), Mockito.anyInt()))
                .thenReturn(List.of());
        Mockito.when(poiService.createAndAddPlacesToItinerary(Mockito.any(), Mockito.any()))
                .thenReturn(List.of());

        // When
        mockMvc.perform(post("/api/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());

        // Then - Verify database storage
        List<ItineraryEntity> savedItineraries = itineraryRepository.findAll();
        assertThat(savedItineraries).hasSize(1);

        ItineraryEntity savedItinerary = savedItineraries.get(0);
        assertThat(savedItinerary.getId()).isNotNull();
        assertThat(savedItinerary.getDestinationCity()).isEqualTo("San Francisco");
        assertThat(savedItinerary.getStartDate()).isEqualTo(request.getStartDate());
        assertThat(savedItinerary.getEndDate()).isEqualTo(request.getEndDate());
        assertThat(savedItinerary.getTravelMode()).isEqualTo(TravelMode.DRIVING);
        assertThat(savedItinerary.getBudgetInCents()).isEqualTo(500000);
        assertThat(savedItinerary.getDailyStart().toString()).isEqualTo("09:00");
        assertThat(savedItinerary.getDailyEnd().toString()).isEqualTo("21:00");
        assertThat(savedItinerary.getCreatedAt()).isNotNull();
        assertThat(savedItinerary.getUpdatedAt()).isNotNull();
        assertThat(savedItinerary.getAiMetadata()).isNotNull();
        assertThat(savedItinerary.getAiMetadata().get("staying_days")).isEqualTo(4);
        assertThat(savedItinerary.getAiMetadata().get("recommended_poi_count")).isEqualTo(12);
    }
}