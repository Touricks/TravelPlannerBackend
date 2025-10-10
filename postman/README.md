# AI Trip Planner - Postman Collections

This directory contains organized Postman collections for testing the AI Trip Planner API.

## Collections Overview

The main collection has been decomposed into separate, focused collections for better organization and maintainability:

### 01 - User Authentication
**File:** `01_authentication.postman_collection.json`

Covers user authentication flows:
- User Registration
- User Login (multiple user roles)
- Password Reset (Forgot Password & Reset Password)
- Authentication error handling

**Endpoints:**
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

### 02 - Recommendations
**File:** `02_recommendations.postman_collection.json`

Tests recommendation retrieval and statistics:
- Get recommendations with various filters
- Pagination support
- Query parameters
- Recommendation statistics

**Endpoints:**
- `GET /api/v1/itineraries/{itineraryId}/recommendations`
- `GET /api/v1/itineraries/{itineraryId}/recommendations/stats`

### 03 - Interest Management
**File:** `03_interests.postman_collection.json`

Tests user interest and place pinning functionality:
- Add interests to places
- Pin/unpin places
- Update existing interests
- Authorization and ownership validation

**Endpoints:**
- `POST /api/itineraries/interests`

### 04 - Itinerary
**File:** `04_itinerary.postman_collection.json`

Tests itinerary creation and management:
- Create itineraries with AI recommendations
- Travel mode preferences
- Budget and date validation

**Endpoints:**
- `POST /api/itineraries`

### 05 - Planning
**File:** `05_planning.postman_collection.json`

Tests itinerary planning and optimization:
- Generate day-by-day plans
- Different travel modes (WALKING, TRANSIT, DRIVING)
- Time-based scheduling
- Route optimization

**Endpoints:**
- `POST /v1/itineraries/{itineraryId}/plan`

## Environment Configuration

**File:** `ai_trip_planner.postman_environment.json`

### Variables:
- `baseUrl` - API base URL (default: http://localhost:8080)
- `authToken` - JWT token for authenticated user
- `adminAuthToken` - JWT token for admin user
- `differentUserAuthToken` - JWT token for different user (ownership tests)
- `itineraryId` - Sample itinerary UUID
- `samplePlaceId` - Sample Google Place ID
- `samplePlaceId2` - Second sample Google Place ID
- `nonexistentItineraryId` - UUID for testing 404 scenarios
- `nonexistentPlaceId` - Invalid Place ID for testing
- `resetToken` - Password reset token

## Setup Instructions

1. **Import Collections:**
   - Import all 5 collection files into Postman
   - Import the environment file

2. **Configure Environment:**
   - Select "AI Trip Planner - Environment" from the environment dropdown
   - Update `baseUrl` if your server runs on a different port

3. **Running Tests:**
   - **Recommended Order:**
     1. Authentication (to get auth tokens)
     2. Itinerary (to create test itineraries)
     3. Recommendations
     4. Interests
     5. Planning

4. **Authentication Flow:**
   - Run a login request first to populate `authToken`
   - The token is automatically saved to the environment
   - Subsequent requests use this token for authorization

## Test Coverage

Each collection includes:
- ✅ Happy path scenarios
- ✅ Validation error cases
- ✅ Authorization/Authentication errors
- ✅ Invalid input handling
- ✅ Response time assertions
- ✅ Response structure validation

## Running Collections

### Single Collection:
```bash
newman run 01_authentication.postman_collection.json \
  -e ai_trip_planner.postman_environment.json
```

### All Collections:
```bash
for collection in *.postman_collection.json; do
  newman run "$collection" -e ai_trip_planner.postman_environment.json
done
```

## Notes

- Collections are numbered for recommended execution order
- Authentication tokens expire after 24 hours (configurable in JWT settings)
- Some tests depend on database state (ensure fresh state for consistent results)
- Password reset tests require email service to be configured

## Legacy Files

- `postman_testkit.json` - Original monolithic collection (deprecated)
- `postman_testkit.json.backup` - Backup of original collection

## Sample Test Data

For Atlanta test queries:
- Museums: Art museum (edge case test)
- Mountains: Stone Mountain (edge case test)

## Contributing

When adding new endpoints:
1. Add tests to the appropriate collection
2. Update this README with new endpoint documentation
3. Add any new environment variables to the environment file
4. Ensure tests include both success and error scenarios

