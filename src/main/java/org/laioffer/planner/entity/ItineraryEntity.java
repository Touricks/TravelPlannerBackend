package org.laioffer.planner.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.laioffer.planner.model.itinerary.TravelMode;
import org.laioffer.planner.user.UserEntity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "itineraries")
@EntityListeners(AuditingEntityListener.class)
public class ItineraryEntity {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    @Column(name = "destination_city", nullable = false)
    private String destinationCity;
    
    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;
    
    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "travel_mode")
    private TravelMode travelMode;
    
    @Column(name = "budget_in_cents")
    private Integer budgetInCents;
    
    @Column(name = "daily_start")
    private LocalTime dailyStart;
    
    @Column(name = "daily_end")
    private LocalTime dailyEnd;
    
    // JSONB fields for AI recommendations
    @Type(JsonType.class)
    @Column(name = "seeded_recommendations", columnDefinition = "jsonb")
    private Map<String, Object> seededRecommendations;
    
    @Type(JsonType.class)
    @Column(name = "ai_metadata", columnDefinition = "jsonb")
    private Map<String, Object> aiMetadata;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ItineraryPlaceEntity> itineraryPlaces = new HashSet<>();
    
    // Constructors
    public ItineraryEntity() {}
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    
    public String getDestinationCity() { return destinationCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }
    
    public OffsetDateTime getStartDate() { return startDate; }
    public void setStartDate(OffsetDateTime startDate) { this.startDate = startDate; }
    
    public OffsetDateTime getEndDate() { return endDate; }
    public void setEndDate(OffsetDateTime endDate) { this.endDate = endDate; }
    
    public TravelMode getTravelMode() { return travelMode; }
    public void setTravelMode(TravelMode travelMode) { this.travelMode = travelMode; }
    
    public Integer getBudgetInCents() { return budgetInCents; }
    public void setBudgetInCents(Integer budgetInCents) { this.budgetInCents = budgetInCents; }
    
    public LocalTime getDailyStart() { return dailyStart; }
    public void setDailyStart(LocalTime dailyStart) { this.dailyStart = dailyStart; }
    
    public LocalTime getDailyEnd() { return dailyEnd; }
    public void setDailyEnd(LocalTime dailyEnd) { this.dailyEnd = dailyEnd; }
    
    public Map<String, Object> getSeededRecommendations() { return seededRecommendations; }
    public void setSeededRecommendations(Map<String, Object> seededRecommendations) { this.seededRecommendations = seededRecommendations; }
    
    public Map<String, Object> getAiMetadata() { return aiMetadata; }
    public void setAiMetadata(Map<String, Object> aiMetadata) { this.aiMetadata = aiMetadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    public Set<ItineraryPlaceEntity> getItineraryPlaces() { return itineraryPlaces; }
    public void setItineraryPlaces(Set<ItineraryPlaceEntity> itineraryPlaces) { this.itineraryPlaces = itineraryPlaces; }
    
    // Helper methods for managing the relationship
    public void addPlace(PlaceEntity place) {
        addPlace(place, false, null);
    }
    
    public void addPlace(PlaceEntity place, boolean pinned) {
        addPlace(place, pinned, null);
    }
    
    public void addPlace(PlaceEntity place, boolean pinned, String note) {
        ItineraryPlaceEntity itineraryPlace = new ItineraryPlaceEntity(this, place, pinned, note);
        itineraryPlaces.add(itineraryPlace);
        place.getItineraryPlaces().add(itineraryPlace);
    }
    
    public void removePlace(PlaceEntity place) {
        ItineraryPlaceEntity itineraryPlace = itineraryPlaces.stream()
            .filter(ip -> ip.getPlace().equals(place))
            .findFirst()
            .orElse(null);
        if (itineraryPlace != null) {
            itineraryPlaces.remove(itineraryPlace);
            place.getItineraryPlaces().remove(itineraryPlace);
            itineraryPlace.setItinerary(null);
            itineraryPlace.setPlace(null);
        }
    }
}