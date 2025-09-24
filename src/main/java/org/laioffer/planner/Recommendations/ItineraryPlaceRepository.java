package org.laioffer.planner.Recommendations;

import org.laioffer.planner.entity.ItineraryPlaceEntity;
import org.laioffer.planner.entity.ItineraryPlaceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItineraryPlaceRepository extends JpaRepository<ItineraryPlaceEntity, ItineraryPlaceId> {
    
    /**
     * Find all places for a specific itinerary
     */
    List<ItineraryPlaceEntity> findByIdItineraryId(UUID itineraryId);
    
    /**
     * Check if a specific place is already added to an itinerary
     */
    boolean existsByIdItineraryIdAndIdPlaceId(UUID itineraryId, UUID placeId);
    
    /**
     * Find places by itinerary and pinned status
     */
    List<ItineraryPlaceEntity> findByIdItineraryIdAndPinned(UUID itineraryId, boolean pinned);
    
    /**
     * Find pinned places for an itinerary
     */
    default List<ItineraryPlaceEntity> findPinnedPlaces(UUID itineraryId) {
        return findByIdItineraryIdAndPinned(itineraryId, true);
    }
    
    /**
     * Find unpinned places for an itinerary
     */
    default List<ItineraryPlaceEntity> findUnpinnedPlaces(UUID itineraryId) {
        return findByIdItineraryIdAndPinned(itineraryId, false);
    }
    
    /**
     * Delete a specific place from an itinerary
     */
    @Modifying
    @Transactional
    void deleteByIdItineraryIdAndIdPlaceId(UUID itineraryId, UUID placeId);
    
    /**
     * Count places in an itinerary
     */
    long countByIdItineraryId(UUID itineraryId);
    
    /**
     * Count pinned places in an itinerary
     */
    long countByIdItineraryIdAndPinned(UUID itineraryId, boolean pinned);
    
    /**
     * Get place IDs for an itinerary (useful for recommendation filtering)
     */
    @Query("SELECT ip.id.placeId FROM ItineraryPlaceEntity ip WHERE ip.id.itineraryId = :itineraryId")
    List<UUID> findPlaceIdsByItineraryId(@Param("itineraryId") UUID itineraryId);
    
    /**
     * Get only pinned place IDs for an itinerary (useful for recommendations)
     */
    @Query("SELECT ip.id.placeId FROM ItineraryPlaceEntity ip WHERE ip.id.itineraryId = :itineraryId AND ip.pinned = true")
    List<UUID> findPinnedPlaceIdsByItineraryId(@Param("itineraryId") UUID itineraryId);
    
    /**
     * Update pinned status for a specific itinerary-place combination
     */
    @Modifying
    @Transactional
    @Query("UPDATE ItineraryPlaceEntity ip SET ip.pinned = :pinned WHERE ip.id.itineraryId = :itineraryId AND ip.id.placeId = :placeId")
    void updatePinnedStatus(@Param("itineraryId") UUID itineraryId, 
                           @Param("placeId") UUID placeId, 
                           @Param("pinned") boolean pinned);
    
    /**
     * Update note for a specific itinerary-place combination
     */
    @Modifying
    @Transactional
    @Query("UPDATE ItineraryPlaceEntity ip SET ip.note = :note WHERE ip.id.itineraryId = :itineraryId AND ip.id.placeId = :placeId")
    void updateNote(@Param("itineraryId") UUID itineraryId, 
                    @Param("placeId") UUID placeId, 
                    @Param("note") String note);
}