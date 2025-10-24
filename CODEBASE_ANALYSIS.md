# AITripPlanner: User Preferences Flow Analysis

## Overview
This document details how user preferences (travel_pace, activity_intensity, travel_place) are collected from the frontend, transformed, stored in the database, and mapped between frontend and backend systems.

---

## 1. Frontend User Preference Collection

### A. Frontend Form Components

#### Main Setup Dialog
- **File**: `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/components/travel-setup/TravelSetupDialog.js`
- **Purpose**: Multi-step tabbed dialog collecting all trip preferences
- **Key State Management** (lines 43-58):
  - `startDate`, `endDate`: Travel dates
  - `adults`, `children`, `infants`, `elderly`, `pets`: Traveler counts
  - `budget`: Budget tier selection
  - `preferences`: Object containing user preferences
    - `travelStyle`: Array of selected categories (multi-select)
    - `transportation`: Single travel mode selection (DRIVING/TRANSIT/WALKING/BICYCLING)
    - `travelPace`: Single selection (RELAXED/MODERATE/PACKED)
  - `mustSee`: Additional preferences text
  - `dietaryRestrictions`: Array of dietary options

#### Preferences Component
- **File**: `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/components/travel-setup/TravelPreferences.js`
- **Lines 101-105**: Renders radio buttons for travelPace selection
- **Selected Field Name**: `travelPace` (maps directly to backend enum)

#### Travelers Component
- **File**: `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/components/travel-setup/Travelers.js`
- **Purpose**: Collects traveler information (adults, children, elderly)
- **Fields Collected**:
  - `adults`: Number of adults
  - `children`: Ages 2-12
  - `infants`: Under 2
  - `elderly`: Ages 65+
  - Note: These determine `hasChildren` and `hasElderly` flags

### B. Frontend Options Configuration

**File**: `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/components/travel-setup/travelSetupOptions.js`

**Travel Pace Options** (lines 33-37):
```javascript
export const travelPaceOptions = [
  { id: 'RELAXED', label: 'Relaxed (2 POIs/day)', icon: '🌴' },
  { id: 'MODERATE', label: 'Moderate (4 POIs/day)', icon: '🚶' },
  { id: 'PACKED', label: 'Packed (5 POIs/day)', icon: '🏃' }
];
```
**Note**: `id` field directly maps to `TravelPace` enum values

**Transportation Options** (lines 25-30):
```javascript
export const transportationOptions = [
  { id: 'DRIVING', label: 'Rental Car', icon: '🚗' },
  { id: 'TRANSIT', label: 'Public Transit', icon: '🚇' },
  { id: 'WALKING', label: 'Walking', icon: '🚶‍♀️' },
  { id: 'BICYCLING', label: 'Bicycling', icon: '🚴' }
];
```
**Note**: No ActivityIntensity selector in UI (set to MODERATE by default in API)

**Travel Styles** (lines 10-22):
```javascript
export const travelStyles = [
  { id: 'CULTURE', label: 'Culture', icon: '🏛️' },
  { id: 'HISTORICAL', label: 'Historical Sites', icon: '🏺' },
  { id: 'NATURE', label: 'Nature', icon: '🏔️' },
  // ... more styles
];
```
**Note**: `id` values map directly to `AttractionCategory` enum

---

## 2. Frontend API Transformation

### Data Transformation Logic

**File**: `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/api/index.js`

#### createTrip Function (lines 72-170)

**Step 1: Collect Frontend Payload** (lines 86-105):
```javascript
const payload = {
  destination: destination.name,
  place_id: destination.place_id,
  startDate: startDate.toISOString().split('T')[0], // YYYY-MM-DD
  endDate: endDate.toISOString().split('T')[0],
  travelers: {
    adults, children, infants, elderly, pets,
  },
  budget: budget, // String ID from budgetOptions
  preferences: {
    travelStyle: preferences?.travelStyle.map(o => o.id) || [],
    transportation: preferences?.transportation || '',
    travelPace: preferences?.travelPace || '',
    mustSeePlaces: mustSee || '',
  },
};
```

**Step 2: Calculate Budget** (lines 89-94):
```javascript
const numberOfTravelers = payload.travelers.adults + 
                         payload.travelers.children + 
                         payload.travelers.infants + 
                         (payload.travelers.elderly || 0);
const days = Math.max(1, Math.ceil(
  (new Date(payload.endDate) - new Date(payload.startDate)) / (1000 * 60 * 60 * 24)
));

const budgetOption = budgetOptions.find(opt => opt.id === payload.budget);
const dailyRateCents = budgetOption?.dailyRateCents;
const budgetLimitCents = dailyRateCents ? 
  dailyRateCents * numberOfTravelers * days : null;
```

**Step 3: Transform to Backend Format** (lines 106-120):
```javascript
const backendPayload = {
  destinationCity: payload.destination,
  startDate: `${payload.startDate}T07:00:00+02:00`,
  endDate: `${payload.endDate}T22:00:00+02:00`,
  travelMode: payload.preferences.transportation || null,
  budgetLimitCents: budgetLimitCents,
  travelPace: "MODERATE", // HARDCODED - NOT FROM FORM!
  activityIntensity: "MODERATE", // HARDCODED - NOT FROM FORM!
  preferredCategories: payload.preferences.travelStyle || [],
  numberOfTravelers: numberOfTravelers,
  hasChildren: payload.travelers.children > 0,
  hasElderly: (payload.travelers.elderly || 0) > 0,
  preferPopularAttractions: false,
  additionalPreferences: payload.preferences.mustSeePlaces || null
};
```

**CRITICAL ISSUE**: Lines 112-113 hardcode travelPace and activityIntensity to "MODERATE" instead of using frontend values!

**Step 4: Send to Backend** (lines 125-129):
```javascript
const res = await api.post('/itineraries', backendPayload, {
  headers: {
    'Content-Type': 'application/json'
  }
});
```

---

## 3. Backend API Request Model

### CreateItineraryRequest DTO

**File**: `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/model/itinerary/CreateItineraryRequest.java`

**User Preference Fields** (lines 26-35):
```java
// User preference fields
private TravelPace travelPace;                              // Line 27
private ActivityIntensity activityIntensity;                // Line 29
private List<AttractionCategory> preferredCategories;       // Line 30
private Integer numberOfTravelers;                          // Line 31
private Boolean hasChildren;                                // Line 32
private Boolean hasElderly;                                 // Line 33
private Boolean preferPopularAttractions;                   // Line 34
private String additionalPreferences;                       // Line 35
```

**Related Fields**:
- `String destinationCity` - line 14
- `OffsetDateTime startDate` - line 17
- `OffsetDateTime endDate` - line 20
- `TravelMode travelMode` - line 22
- `Integer budgetLimitCents` - line 24

---

## 4. Backend Service Layer

### ItineraryServiceImpl - Data Mapping

**File**: `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/itinerary/ItineraryServiceImpl.java`

#### createItinerary Method (lines 51-107)

**Mapping Logic** (lines 63-96):
```java
ItineraryEntity itinerary = new ItineraryEntity();
itinerary.setUser(user);
itinerary.setDestinationCity(request.getDestinationCity());
itinerary.setStartDate(request.getStartDate());
itinerary.setEndDate(request.getEndDate());
itinerary.setTravelMode(request.getTravelMode());
itinerary.setBudgetInCents(request.getBudgetLimitCents());

// Map user preference fields
itinerary.setTravelPace(request.getTravelPace());          // Direct mapping
itinerary.setActivityIntensity(request.getActivityIntensity()); // Direct mapping
itinerary.setNumberOfTravelers(request.getNumberOfTravelers());
itinerary.setHasChildren(request.getHasChildren());
itinerary.setHasElderly(request.getHasElderly());
itinerary.setPreferPopularAttractions(request.getPreferPopularAttractions());

// Convert List<AttractionCategory> enum to List<String> for storage
if (request.getPreferredCategories() != null && 
    !request.getPreferredCategories().isEmpty()) {
  itinerary.setPreferredCategories(
    request.getPreferredCategories().stream()
      .map(Enum::name)
      .toList()
  );
} else {
  itinerary.setPreferredCategories(new java.util.ArrayList<>());
}

itinerary.setAdditionalPreferences(request.getAdditionalPreferences());

// Store metadata for POI generation
Map<String, Object> aiMetadata = new HashMap<>();
aiMetadata.put("staying_days", stayingDays);
aiMetadata.put("recommended_poi_count", poiCount);
aiMetadata.put("generation_pending", true);
itinerary.setAiMetadata(aiMetadata);
```

#### POI Count Calculation (lines 211-218)

**Critical: Travel Pace Usage**:
```java
private int calculatePOICount(int stayingDays, TravelPace pace) {
  int poiPerDay = switch (pace) {
    case RELAXED -> 2;      // 2 POIs per day
    case MODERATE -> 4;     // 4 POIs per day
    case PACKED -> 5;       // 5 POIs per day
  };
  return Math.min(stayingDays * poiPerDay, MAX_POI_COUNT); // Max 16
}
```

---

## 5. Database Schema

### Itinerary Table Definition

**Database**: PostgreSQL
**Location**: Schema auto-generated by Hibernate JPA

#### Preference-Related Columns

**File**: `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/entity/ItineraryEntity.java`

**Column Definitions**:
```java
@Enumerated(EnumType.STRING)
@Column(name = "travel_pace", nullable = false)
private TravelPace travelPace;                              // Line 50-51
// DB: VARCHAR(50) with ENUM check constraint

@Enumerated(EnumType.STRING)
@Column(name = "activity_intensity")
private ActivityIntensity activityIntensity;                // Line 53-55
// DB: VARCHAR(50) with ENUM check constraint

@Column(name = "number_of_travelers")
private Integer numberOfTravelers;                          // Line 57-58
// DB: INTEGER

@Column(name = "has_children")
private Boolean hasChildren;                                // Line 60-61
// DB: BOOLEAN

@Column(name = "has_elderly")
private Boolean hasElderly;                                 // Line 63-64
// DB: BOOLEAN

@Column(name = "prefer_popular_attractions")
private Boolean preferPopularAttractions;                   // Line 66-67
// DB: BOOLEAN

@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "itinerary_preferred_categories",
                 joinColumns = @JoinColumn(name = "itinerary_id"))
@Column(name = "category")
private List<String> preferredCategories;                   // Line 69-73
// DB: Join table with VARCHAR(50) category values

@Column(name = "additional_preferences", columnDefinition = "text")
private String additionalPreferences;                       // Line 75-76
// DB: TEXT

@Type(JsonType.class)
@Column(name = "ai_metadata", columnDefinition = "jsonb")
private Map<String, Object> aiMetadata;                     // Line 83-85
// DB: JSONB storing staying_days, recommended_poi_count, generation_pending
```

#### Related Columns
```java
@Enumerated(EnumType.STRING)
@Column(name = "travel_mode")
private TravelMode travelMode;                              // Line 42-43
// DB: VARCHAR(50) - DRIVING/TRANSIT/WALKING/BICYCLING/MIXED

@Column(name = "budget_in_cents", nullable = false)
private Integer budgetInCents;                              // Line 45-46
// DB: INTEGER in cents

@Column(name = "destination_city", nullable = false)
private String destinationCity;                             // Line 32-33
// DB: VARCHAR(255)

@Column(name = "start_date", nullable = false)
private java.time.OffsetDateTime startDate;                // Line 35-36
// DB: TIMESTAMP WITH TIME ZONE

@Column(name = "end_date", nullable = false)
private java.time.OffsetDateTime endDate;                  // Line 38-39
// DB: TIMESTAMP WITH TIME ZONE
```

### Sample Data

**File**: `/Users/carrick/IdeaProjects/AITripPlanner/src/main/resources/import.sql` (lines 30-34)

Sample itinerary creation:
```sql
INSERT INTO itineraries (
  id, user_id, destination_city, start_date, end_date, travel_mode,
  budget_in_cents, travel_pace, activity_intensity, number_of_travelers,
  has_children, has_elderly, prefer_popular_attractions,
  additional_preferences, seeded_recommendations, ai_metadata, created_at, updated_at
) VALUES (
  gen_random_uuid(), 1, 'San Francisco',
  '2024-03-15T09:00:00-07:00', '2024-03-17T18:00:00-07:00',
  'WALKING', 50000,
  'RELAXED', 'LIGHT', 4,                    -- travel_pace, activity_intensity
  true, false, true,
  'Family-friendly activities preferred, need stroller accessibility',
  '{"interests": ["landmarks", "culture", "food"], ...}',
  '{"ai_model": "gpt-4", ...}',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

### Related Tables

#### itinerary_preferred_categories (Join Table)
```sql
CREATE TABLE itinerary_preferred_categories (
  itinerary_id UUID NOT NULL,
  category VARCHAR(50),
  PRIMARY KEY (itinerary_id, category),
  FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE
);
```

#### Enumerated Values
**TravelPace Enum**:
- RELAXED
- MODERATE
- PACKED

**ActivityIntensity Enum**:
- LIGHT
- MODERATE
- INTENSE

**TravelMode Enum**:
- DRIVING
- TRANSIT
- WALKING
- BICYCLING
- MIXED

**AttractionCategory Enum**:
- CULTURE
- HISTORICAL
- NATURE
- ADVENTURE
- FOOD
- SHOPPING
- NIGHTLIFE
- MUSEUM
- ENTERTAINMENT
- SPORTS
- ART

---

## 6. Backend API Response

### ItineraryDetailResponse

**File**: `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/model/itinerary/ItineraryDetailResponse.java`

**Return Mapping** (in ItineraryController.convertToDetailResponse, lines 160-200):
```java
response.setTravelPace(entity.getTravelPace());                    // Line 170
response.setActivityIntensity(entity.getActivityIntensity());      // Line 171
response.setNumberOfTravelers(entity.getNumberOfTravelers());      // Line 172
response.setHasChildren(entity.getHasChildren());                  // Line 173
response.setHasElderly(entity.getHasElderly());                    // Line 174
response.setPreferPopularAttractions(entity.getPreferPopularAttractions()); // Line 175

// Convert preferredCategories from List<String> to List<AttractionCategory>
if (entity.getPreferredCategories() != null) {
  List<AttractionCategory> categories = entity.getPreferredCategories().stream()
    .map(AttractionCategory::valueOf)
    .collect(Collectors.toList());
  response.setPreferredCategories(categories);
}

response.setAdditionalPreferences(entity.getAdditionalPreferences());
```

---

## 7. Data Flow Diagram

```
┌─────────────────────────────────────────┐
│   FRONTEND (React/JavaScript)           │
├─────────────────────────────────────────┤
│  TravelSetupDialog                      │
│  ├─ TravelPreferences.js                │
│  │  └─ preferences.travelPace (STRING)  │
│  ├─ Travelers.js                        │
│  │  ├─ children (NUMBER)                │
│  │  └─ elderly (NUMBER)                 │
│  ├─ TravelBudget.js                     │
│  └─ travelSetupOptions.js               │
│     ├─ travelPaceOptions (ENUM MAP)     │
│     └─ travelStyles (ENUM MAP)          │
└──────────────────┬──────────────────────┘
                   │ createTrip(payload)
                   ▼
┌─────────────────────────────────────────┐
│   API TRANSFORMATION LAYER              │
│   (/api/index.js - createTrip)          │
├─────────────────────────────────────────┤
│  1. Collect frontend values             │
│     preferences.travelPace ──────┐      │
│     preferences.transportation   │      │
│  2. Calculate budget             │      │
│     (per-person daily rate)      │      │
│  3. Transform to backend format  │      │
│     travelPace: "MODERATE" ◄─────┘      │  ISSUE: HARDCODED!
│     activityIntensity: "MODERATE" ◄─ HARDCODED!
│     preferredCategories: []             │
│     numberOfTravelers: count            │
│     hasChildren: boolean                │
│     hasElderly: boolean                 │
│     additionalPreferences: string       │
└──────────────────┬──────────────────────┘
                   │ POST /itineraries
                   ▼
┌─────────────────────────────────────────┐
│   BACKEND API ENDPOINT                  │
│   (ItineraryController.createItinerary) │
├─────────────────────────────────────────┤
│  Receives CreateItineraryRequest        │
│  ├─ travelPace: TravelPace enum         │
│  ├─ activityIntensity: ActivityIntensity│
│  ├─ preferredCategories: List<Enum>     │
│  ├─ numberOfTravelers: Integer          │
│  ├─ hasChildren: Boolean                │
│  ├─ hasElderly: Boolean                 │
│  └─ additionalPreferences: String       │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│   SERVICE LAYER MAPPING                 │
│   (ItineraryServiceImpl.createItinerary) │
├─────────────────────────────────────────┤
│  Direct mapping to ItineraryEntity:     │
│  ├─ request.getTravelPace()             │
│  ├─ request.getActivityIntensity()      │
│  ├─ Convert AttractionCategory[]        │
│  │  → String[] (enum names)             │
│  ├─ request.getNumberOfTravelers()      │
│  ├─ request.getHasChildren()            │
│  ├─ request.getHasElderly()             │
│  └─ request.getAdditionalPreferences()  │
│                                         │
│  Calculate POI count using:             │
│  ├─ stayingDays (from dates)            │
│  └─ travelPace (2/4/5 POIs per day)     │
│                                         │
│  Store in aiMetadata:                   │
│  ├─ staying_days                        │
│  ├─ recommended_poi_count               │
│  └─ generation_pending: true            │
└──────────────────┬──────────────────────┘
                   │ itineraryRepository.save()
                   ▼
┌─────────────────────────────────────────┐
│   DATABASE (PostgreSQL)                 │
├─────────────────────────────────────────┤
│  Table: itineraries                     │
│  ├─ travel_pace VARCHAR(50)             │
│  │  [RELAXED|MODERATE|PACKED]           │
│  ├─ activity_intensity VARCHAR(50)      │
│  │  [LIGHT|MODERATE|INTENSE]            │
│  ├─ number_of_travelers INTEGER         │
│  ├─ has_children BOOLEAN                │
│  ├─ has_elderly BOOLEAN                 │
│  ├─ prefer_popular_attractions BOOLEAN  │
│  ├─ additional_preferences TEXT         │
│  └─ ai_metadata JSONB                   │
│     {                                   │
│       "staying_days": 5,                │
│       "recommended_poi_count": 16,      │
│       "generation_pending": true        │
│     }                                   │
│                                         │
│  Table: itinerary_preferred_categories  │
│  ├─ itinerary_id UUID (FK)              │
│  └─ category VARCHAR(50)                │
│     [CULTURE|HISTORICAL|NATURE|...]     │
└─────────────────────────────────────────┘
```

---

## 8. Summary of Field Mappings

### Frontend → Backend Mapping

| Frontend Field | Frontend Type | API Field | Request Field | DB Column | DB Type | Enum Values |
|---|---|---|---|---|---|---|
| preferences.travelPace | String (single) | travelPace | TravelPace | travel_pace | VARCHAR(50) | RELAXED, MODERATE, PACKED |
| preferences.transportation | String (single) | N/A | travelMode in TravelMode | travel_mode | VARCHAR(50) | DRIVING, TRANSIT, WALKING, BICYCLING, MIXED |
| preferences.travelStyle | String[] (multi) | preferredCategories | List<AttractionCategory> | itinerary_preferred_categories | Join table | CULTURE, HISTORICAL, NATURE, etc. |
| travelers.children > 0 | Boolean | hasChildren | Boolean | has_children | BOOLEAN | true/false |
| travelers.elderly > 0 | Boolean | hasElderly | Boolean | has_elderly | BOOLEAN | true/false |
| travelers total | Integer | numberOfTravelers | Integer | number_of_travelers | INTEGER | 1-16 |
| budget tier | String (ID) | budgetLimitCents | Integer | budget_in_cents | INTEGER | Cents (int) |
| mustSeePlaces | String | additionalPreferences | String | additional_preferences | TEXT | Free text |
| N/A (not in UI) | N/A | N/A | ActivityIntensity | activity_intensity | VARCHAR(50) | LIGHT, MODERATE, INTENSE |

### Special Handling

**ActivityIntensity**: 
- NOT collected from frontend form
- HARDCODED to "MODERATE" in API transformation (line 113 of index.js)
- Should be enhanced with UI form if needed

**TravelPace**:
- Collected from frontend TravelPreferences component
- Uses TravelPace enum directly
- SHOULD be passed from frontend but currently HARDCODED to "MODERATE" (line 112 of index.js)
- **BUG**: User selection is ignored; always creates with MODERATE pace

**POI Calculation** (relies on travelPace):
```
RELAXED  → 2 POIs/day
MODERATE → 4 POIs/day
PACKED   → 5 POIs/day
Maximum: 16 POIs total
```

---

## 9. Key Issues and Recommendations

### Issue 1: travelPace Hardcoding
**Location**: `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/api/index.js:112`

**Current Code**:
```javascript
travelPace: "MODERATE", // HARDCODED - NOT FROM FORM!
```

**Should Be**:
```javascript
travelPace: payload.preferences.travelPace || "MODERATE",
```

**Impact**: POI count calculation always uses MODERATE (4 POIs/day) regardless of user selection

### Issue 2: activityIntensity Missing from UI
**Location**: No ActivityIntensity form control in TravelSetupDialog

**Current Behavior**: Hardcoded to "MODERATE" in API (line 113)

**Fix**: Add ActivityIntensity selector to TravelPreferences component with enum values:
- LIGHT (minimal walking)
- MODERATE (standard activities)
- INTENSE (hiking/sports)

### Issue 3: Inconsistent Enum Naming
- Frontend: `travelPace` (camelCase)
- Backend: `TravelPace` (PascalCase)
- Database: `travel_pace` (snake_case)
- This is handled correctly but could be documented better

---

## 10. File Reference Summary

### Backend Files

**Enums**:
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/model/common/TravelPace.java`
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/model/common/ActivityIntensity.java`
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/model/itinerary/TravelMode.java`
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/model/common/AttractionCategory.java`

**Request/Response DTOs**:
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/model/itinerary/CreateItineraryRequest.java`
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/model/itinerary/CreateItineraryResponse.java`

**Entities**:
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/entity/ItineraryEntity.java`
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/entity/ItineraryPlaceEntity.java`

**Service/Controller**:
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/itinerary/ItineraryController.java`
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/itinerary/ItineraryServiceImpl.java`
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/java/org/laioffer/planner/itinerary/ItineraryService.java`

**Database**:
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/resources/application.yml`
- `/Users/carrick/IdeaProjects/AITripPlanner/src/main/resources/import.sql`

### Frontend Files

**Components**:
- `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/components/travel-setup/TravelSetupDialog.js`
- `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/components/travel-setup/TravelPreferences.js`
- `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/components/travel-setup/Travelers.js`
- `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/components/travel-setup/TravelBudget.js`

**Configuration**:
- `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/components/travel-setup/travelSetupOptions.js`

**API**:
- `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/api/index.js`

**Pages**:
- `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/pages/Home/index.js`
- `/Users/carrick/IdeaProjects/AITripPlanner/AITripPlannerFE/trip-mate/src/pages/Setup/index.js`

