# Itineraries Module Design Documentation

## Overview
The Itineraries module handles creation of travel itineraries with AI-powered POI recommendations using SpringAI and Gemini model integration.

## Core Endpoint
**POST /itineraries** - Creates new itinerary with AI-generated place recommendations

## Request/Response Flow
```
CreateItineraryRequest → Calculate Days → AI Prompt → Gemini → Parse POIs → Save → 201 Created
```

## Business Logic

### 1. Staying Days Calculation
```java
int stayingDays = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate());
```

### 2. POI Recommendation Count
```java
int recommendedPOICount = Math.min(stayingDays * 3, 15);
// Examples: 2 days = 6 POIs, 5 days = 15 POIs (capped), 10 days = 15 POIs (capped)
```

## Data Models

### CreateItineraryRequest (from OpenAPI)
```java
{
  "destinationCity": "string",
  "startDate": "2025-10-01T15:00:00+08:00",  // ISO 8601 with timezone
  "endDate": "2025-10-05T20:00:00+08:00",
  "travelMode": "DRIVING|TRANSIT|WALKING|BICYCLING",
  "budgetLimitCents": 50000,  // optional
  "dailyStart": "09:00",      // HH:mm format
  "dailyEnd": "20:00"
}
```

### Response
- **HTTP 201 Created** status
- **Location header** with itinerary resource URL
- **No response body** (per OpenAPI spec)

## AI Integration Architecture

### SpringAI Prompt Template
```
You are a travel expert. Generate ${count} POI recommendations for:
- Destination: ${destinationCity}
- Duration: ${stayingDays} days  
- Travel mode: ${travelMode}
- Budget: ${budget} cents
- Daily schedule: ${dailyStart} to ${dailyEnd}

Return as JSON array matching our Places table schema:
[{
  "name": "string",
  "address": "string",
  "latitude": number,
  "longitude": number, 
  "description": "string",
  "opening_hours": {"monday": {"open": "09:00", "close": "17:00"}, ...},
  "contact_info": {"website": "url", "phone": "number"}
}]
```

### Response Parsing Strategy
1. Parse JSON response from Gemini
2. Validate required fields (name, address, coordinates)
3. Convert to PlaceEntity objects
4. Save to database with JSONB fields
5. Link via ItineraryPlaceEntity

## Component Structure

### Controllers
- `ItineraryController` - Handles POST /itineraries endpoint

### Services
- `ItineraryService` - Main orchestration logic
- `AIRecommendationService` - SpringAI/Gemini integration
- `PlaceParsingService` - Natural language → Entity conversion

### Repositories
- `ItineraryRepository` - Itinerary CRUD operations
- `PlaceRepository` - Place management (existing)

## Service Distribution & Contracts

### 1. ItineraryService (Orchestrator)
**Responsibilities:**
- Main business logic orchestration
- Input validation and date calculations
- Transaction management
- Error handling and fallback logic

**Contract:**
```java
public interface ItineraryService {
    UUID createItinerary(CreateItineraryRequest request);
    
    // Internal methods
    private int calculateStayingDays(LocalDateTime start, LocalDateTime end);
    private int calculatePOICount(int stayingDays);
    private void validateRequest(CreateItineraryRequest request);
}
```

### 2. AIRecommendationService (AI Integration)
**Responsibilities:**
- SpringAI/Gemini model integration
- Prompt generation and templating
- Raw AI response handling
- Model configuration and error handling

**Contract:**
```java
public interface AIRecommendationService {
    String generatePOIRecommendations(AIPromptRequest promptRequest);
    
    // Data contract
    public class AIPromptRequest {
        private String destinationCity;
        private int stayingDays;
        private int poiCount;
        private TravelMode travelMode;
        private Integer budgetCents;
        private String dailyStart;
        private String dailyEnd;
    }
}
```

### 3. PlaceParsingService (Data Processing)
**Responsibilities:**
- Parse AI JSON/natural language response
- Data validation and sanitization
- PlaceEntity creation and mapping
- Database persistence coordination

**Contract:**
```java
public interface PlaceParsingService {
    List<PlaceEntity> parseAndSavePlaces(String aiResponse, UUID itineraryId);
    
    // Internal contracts
    private List<PlaceData> parseAIResponse(String response);
    private PlaceEntity convertToEntity(PlaceData data);
    private void validatePlaceData(PlaceData data);
}
```

## Service Interaction Flow

```
ItineraryController
       ↓
ItineraryService.createItinerary()
   ├── validateRequest()
   ├── calculateStayingDays() 
   ├── calculatePOICount()
   ├── AIRecommendationService.generatePOIRecommendations()
   ├── PlaceParsingService.parseAndSavePlaces()
   └── saveItinerary()
```

## Key Contracts to Define

### 1. Data Contracts
```java
// Between ItineraryService → AIRecommendationService
public class AIPromptRequest {
    // All required fields for prompt generation
}

// Between AIRecommendationService → PlaceParsingService  
public class AIResponse {
    private String rawResponse;
    private boolean isValid;
    private String errorMessage;
}

// Between PlaceParsingService → Database
public class PlaceData {
    // Validated, parsed place information
}
```

### 2. Error Handling Contracts
```java
public class AIServiceException extends RuntimeException {
    private AIErrorType type; // MODEL_UNAVAILABLE, QUOTA_EXCEEDED, INVALID_RESPONSE
}

public class PlaceParsingException extends RuntimeException {
    private List<String> validationErrors;
}
```

### 3. Configuration Contracts
```java
@ConfigurationProperties("app.ai")
public class AIConfiguration {
    private String model;
    private int maxTokens;
    private double temperature;
    private String promptTemplate;
}
```

## Critical Design Decisions

### Transaction Boundaries
- **ItineraryService** manages the entire transaction
- If AI pipeline fails, we should not create the itinerary

### Retry Logic
- **AIRecommendationService** handles model retries, requested by ItineraryService with proper prompt if place parsing fails. Set Maxnumber as 3.
- **PlaceParsingService** return error information if problem encounter.

### Caching Strategy
- No caching Strategy Implement.

### Async Processing
AI generation should be synchronous with timeout=1min

## Database Integration

### Entities Created
1. **ItineraryEntity** - Main itinerary record
2. **PlaceEntity** - AI-generated POI recommendations  
3. **ItineraryPlaceEntity** - Links itinerary to recommended places

### JSONB Field Usage
- `opening_hours` - Structured time data from AI
- `contact_info` - Website, phone from AI
- `metadata` - Additional AI-generated context

## Error Handling
- **400 Bad Request** - Invalid date ranges, malformed input
- **500 Internal Server Error** - AI service failures, database errors
- **Fallback mechanisms** - Default recommendations if AI fails

## Future Enhancements
- GoogleMap/BaiduMap MCP integration for data enrichment
- Caching of popular destination recommendations
- User preference learning and personalization

This design provides a complete AI-powered itinerary creation system that generates personalized POI recommendations based on user travel preferences and constraints.