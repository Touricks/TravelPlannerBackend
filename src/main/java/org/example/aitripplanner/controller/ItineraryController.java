package org.example.aitripplanner.controller;

import jakarta.validation.Valid;
import org.example.aitripplanner.dto.CreateItineraryRequest;
import org.example.aitripplanner.dto.CreateItineraryResponse;
import org.example.aitripplanner.service.ItineraryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/itineraries")
public class ItineraryController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @PostMapping
    public ResponseEntity<CreateItineraryResponse> createItinerary(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody CreateItineraryRequest request) {

        String userId = extractUserId(authorization);
        CreateItineraryResponse response = itineraryService.createItinerary(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String extractUserId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid bearer token");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity in bearer token");
        }
        return token;
    }
}
