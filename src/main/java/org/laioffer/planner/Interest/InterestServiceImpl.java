package org.laioffer.planner.Interest;

import org.laioffer.planner.Recommendation.PlaceMapper;
import org.laioffer.planner.model.place.PlaceDTO;
import org.laioffer.planner.entity.ItineraryEntity;
import org.laioffer.planner.entity.ItineraryPlaceEntity;
import org.laioffer.planner.entity.UserEntity;
import org.laioffer.planner.repository.ItineraryPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InterestServiceImpl implements InterestService {

    private static final Logger logger = LoggerFactory.getLogger(InterestServiceImpl.class);

    private final ItineraryPlaceRepository itineraryPlaceRepository;
    private final PlaceMapper placeMapper;

    public InterestServiceImpl(
            ItineraryPlaceRepository itineraryPlaceRepository,
            PlaceMapper placeMapper) {
        this.itineraryPlaceRepository = itineraryPlaceRepository;
        this.placeMapper = placeMapper;
    }
    
    @Override
    @Transactional
    public AddInterestResponse addInterest(AddInterestRequest request, UserEntity user) {
        if (request == null || request.getItineraryPlaceId() == null) {
            throw new IllegalArgumentException("itineraryPlaceId must be provided");
        }

        if (user == null || user.getId() == null) {
            throw new SecurityException("Authenticated user is required");
        }

        UUID itineraryPlaceId = request.getItineraryPlaceId();

        logger.info("Processing interest for itineraryPlaceId: {}, user: {}",
                itineraryPlaceId, user.getEmail());

        ItineraryPlaceEntity itineraryPlace = itineraryPlaceRepository
                .findById(itineraryPlaceId)
                .orElseThrow(() -> new RuntimeException(
                        "Itinerary place not found: " + itineraryPlaceId));

        ItineraryEntity itinerary = itineraryPlace.getItinerary();
        if (itinerary == null || itinerary.getUser() == null) {
            throw new RuntimeException(
                    "Itinerary not found for itinerary place: " + itineraryPlaceId);
        }

        Long ownerId = itinerary.getUser().getId();
        if (ownerId == null || !ownerId.equals(user.getId())) {
            logger.warn("Unauthorized access attempt by user {} on itineraryPlace {}",
                    user.getEmail(), itineraryPlaceId);
            throw new SecurityException(
                    "User does not own the itinerary for the requested place");
        }

        itineraryPlace.setPinned(request.isPinned());
        ItineraryPlaceEntity updatedItineraryPlace = itineraryPlaceRepository.save(itineraryPlace);

        logger.info("Updated pinned status to {} for itineraryPlace: {}",
                request.isPinned(), itineraryPlaceId);

        PlaceDTO placeDTO = placeMapper.toItineraryPlaceDTO(updatedItineraryPlace);

        return new AddInterestResponse(placeDTO, updatedItineraryPlace.isPinned());
    }
}
