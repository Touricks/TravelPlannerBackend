package org.example.aitripplanner.dto;

import java.util.UUID;

public record PlaceDto(
        UUID id,
        String name,
        String address
) {
}
