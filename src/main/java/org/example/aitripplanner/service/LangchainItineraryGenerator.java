package org.example.aitripplanner.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.example.aitripplanner.dto.CreateItineraryRequest;
import org.example.aitripplanner.dto.PlaceDto;
import org.example.aitripplanner.dto.TravelMode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class LangchainItineraryGenerator implements ItineraryGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;
    private final ChatLanguageModel chatModel;

    public LangchainItineraryGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.chatModel = buildModel();
    }

    @Override
    public List<PlaceDto> generatePlaces(CreateItineraryRequest request) {
        if (chatModel == null) {
            return fallbackRecommendations(request);
        }

        String prompt = buildPrompt(request);
        try {
            String raw = chatModel.generate(prompt);
            LangchainResponse response = objectMapper.readValue(raw, LangchainResponse.class);
            if (response == null || response.places == null || response.places.isEmpty()) {
                return fallbackRecommendations(request);
            }
            return response.places.stream()
                    .filter(place -> StringUtils.hasText(place.name()))
                    .map(place -> new PlaceDto(
                            toDeterministicId(request.destinationCity(), place.name()),
                            place.name(),
                            place.address()))
                    .toList();
        } catch (Exception ex) {
            return fallbackRecommendations(request);
        }
    }

    private ChatLanguageModel buildModel() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .temperature(0.4)
                .build();
    }

    private String buildPrompt(CreateItineraryRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are a helpful travel planner. Based on the user's trip details, suggest 3 interesting places to visit.\n");
        builder.append("Return ONLY a JSON object that matches this schema: {\"places\": [{\"name\": string, \"address\": string}]}. No extra commentary.\n");
        builder.append("Trip details:\n");
        builder.append("Destination: ").append(request.destinationCity()).append("\n");
        builder.append("Start: ").append(formatDate(request.startDate())).append("\n");
        builder.append("End: ").append(formatDate(request.endDate())).append("\n");
        builder.append("Travel mode: ").append(formatTravelMode(request.travelMode())).append("\n");
        builder.append("Daily window: ").append(formatDailyWindow(request.dailyStart(), request.dailyEnd())).append("\n");
        builder.append("Budget (cents): ").append(request.budgetLimitCents() != null ? request.budgetLimitCents() : "unknown").append("\n");
        return builder.toString();
    }

    private List<PlaceDto> fallbackRecommendations(CreateItineraryRequest request) {
        return List.of(
                new PlaceDto(toDeterministicId(request.destinationCity(), "City Highlights Tour"),
                        request.destinationCity() + " City Highlights Tour",
                        request.destinationCity() + " downtown"),
                new PlaceDto(toDeterministicId(request.destinationCity(), "Local Cuisine Experience"),
                        request.destinationCity() + " Local Cuisine Experience",
                        request.destinationCity() + " food district")
        );
    }

    private UUID toDeterministicId(String destinationCity, String name) {
        String key = destinationCity + ":" + name;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private String formatDate(OffsetDateTime date) {
        return date != null ? DATE_FORMATTER.format(date) : "unknown";
    }

    private String formatTravelMode(TravelMode travelMode) {
        return travelMode != null ? travelMode.name() : "unspecified";
    }

    private String formatDailyWindow(java.time.LocalTime dailyStart, java.time.LocalTime dailyEnd) {
        if (dailyStart == null && dailyEnd == null) {
            return "not provided";
        }
        return (dailyStart != null ? dailyStart : "??") + " - " + (dailyEnd != null ? dailyEnd : "??");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LangchainResponse {
        private List<LangchainPlace> places;

        public List<LangchainPlace> getPlaces() {
            return places;
        }

        public void setPlaces(List<LangchainPlace> places) {
            this.places = places;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LangchainPlace {
        private String name;
        private String address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }
}
