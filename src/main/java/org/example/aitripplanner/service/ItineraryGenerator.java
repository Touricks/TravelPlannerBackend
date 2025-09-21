package org.example.aitripplanner.service;

import org.example.aitripplanner.dto.CreateItineraryRequest;
import org.example.aitripplanner.dto.PlaceDto;

import java.util.List;

public interface ItineraryGenerator {

    List<PlaceDto> generatePlaces(CreateItineraryRequest request);
}
