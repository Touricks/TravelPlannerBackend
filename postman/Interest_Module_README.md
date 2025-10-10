# Interest Module - Postman Test Kit

This document describes the Postman test collection for the Interest Management module of the AI Trip Planner API.

## Overview

The Interest Management module allows authenticated users to add places to their itinerary's interest list, with the ability to mark places as pinned/favorite. The module includes comprehensive validation and security checks.

## API Endpoint

**POST** `/api/itineraries/interests`

- **Authentication**: Required (Bearer JWT token)
- **Content-Type**: `application/json`

### Request Body

```json
{
    "Itinerary_placeId": "string",  // Format: "itineraryId_placeId"
    "pinned": boolean               // Optional, defaults to false
}
```

### Response (200 OK)

```json
{
    "place": {
        "id": "uuid",
        "name": "string",
        "address": "string",
        "location": {
            "lat": number,
            "lng": number
        },
        "contact": {
            "website": "string",
            "phone": "string"
        },
        "imageUrl": "string",
        "description": "string",
        "openingHours": {
            "raw": "string",
            "normalized": [...]
        },
        "itineraryPlaceRecordId": "uuid",
        "pinned": boolean,
        "note": "string",
        "addedAt": "datetime"
    },
    "pinned": boolean
}
```

## Test Collection Structure

The Interest Management test collection includes the following test scenarios:

### 1. **Positive Test Cases**

#### Add Interest - Basic Request
- Tests successful addition of a place to interest list
- Validates response structure
- Checks that pinned status matches request
- Verifies response time performance

#### Add Interest - Pinned Place
- Tests adding a place with pinned=true
- Validates that the place is correctly marked as pinned
- Confirms response structure integrity

#### Add Interest - Update Existing Place
- Tests updating an existing place's pinned status
- Validates that existing relationships are updated correctly
- Ensures place ID consistency

### 2. **Validation Test Cases**

#### Invalid Itinerary_placeId Format
- Tests handling of invalid format (missing underscore)
- Expects HTTP 400 Bad Request

#### Invalid UUID in Itinerary_placeId
- Tests handling of malformed UUIDs
- Expects HTTP 400 Bad Request

### 3. **Security Test Cases**

#### Unauthorized Access
- Tests endpoint without authentication token
- Expects HTTP 401 Unauthorized

#### User Does Not Own Itinerary
- Tests access with different user's token
- Expects HTTP 403 Forbidden
- Uses `{{differentUserAuthToken}}` variable

### 4. **Error Handling Test Cases**

#### Nonexistent Itinerary
- Tests with non-existent itinerary ID
- Expects HTTP 404 Not Found

#### Nonexistent Place
- Tests with non-existent place ID
- Expects HTTP 404 Not Found

## Environment Variables

The test collection uses the following environment variables:

| Variable | Description | Example Value |
|----------|-------------|---------------|
| `baseUrl` | API base URL | `http://localhost:8080` |
| `itineraryId` | Valid itinerary UUID for testing | `550e8400-e29b-41d4-a716-446655440000` |
| `authToken` | JWT token for authenticated user | Set during authentication |
| `samplePlaceId` | Valid place UUID for testing | `123e4567-e89b-12d3-a456-426614174001` |
| `samplePlaceId2` | Second valid place UUID | `123e4567-e89b-12d3-a456-426614174002` |
| `nonexistentItineraryId` | Invalid itinerary UUID for negative tests | `999e9999-e99b-99d9-a999-999999999999` |
| `nonexistentPlaceId` | Invalid place UUID for negative tests | `888e8888-e88b-88d8-a888-888888888888` |
| `differentUserAuthToken` | JWT token for unauthorized user testing | Set during test setup |

## Test Setup Instructions

### 1. Authentication Setup

Before running the Interest tests, you need to authenticate and obtain JWT tokens:

```javascript
// In a pre-request script or authentication test
pm.test("Login successful", function () {
    const responseJson = pm.response.json();
    pm.environment.set("authToken", responseJson.token);
});
```

### 2. Test Data Setup

Ensure the following test data exists in your database:

- **Valid Itinerary**: Owned by the authenticated user
- **Valid Places**: At least 2 places that can be added to interests
- **Different User**: For testing unauthorized access scenarios

### 3. Running the Tests

1. Import the collection: `Recommendations_API_Tests.postman_collection.json`
2. Import the environment: `AI_Trip_Planner_Environment.postman_environment.json`
3. Set up authentication tokens
4. Run the "Interest Management" folder

## Expected Test Results

| Test Case | Expected Status | Expected Behavior |
|-----------|----------------|-------------------|
| Basic Request | 200 OK | Place added to interests |
| Pinned Place | 200 OK | Place added with pinned=true |
| Update Existing | 200 OK | Pinned status updated |
| Invalid Format | 400 Bad Request | Validation error |
| Invalid UUID | 400 Bad Request | UUID parsing error |
| No Auth | 401 Unauthorized | Authentication required |
| Wrong User | 403 Forbidden | Authorization denied |
| No Itinerary | 404 Not Found | Itinerary not found |
| No Place | 404 Not Found | Place not found |

## Performance Benchmarks

- **Response Time**: < 2000ms for successful requests
- **Validation Time**: < 500ms for validation errors
- **Authentication**: < 300ms for auth checks

## Integration Notes

The Interest Management tests integrate with:

- **Authentication System**: JWT token validation
- **Authorization**: User ownership verification
- **Database**: ItineraryPlaceEntity CRUD operations
- **Data Mapping**: PlaceDTO transformation

## Troubleshooting

### Common Issues

1. **401 Unauthorized**: Ensure `authToken` is set correctly
2. **403 Forbidden**: Verify user owns the test itinerary
3. **404 Not Found**: Check that test UUIDs exist in database
4. **400 Bad Request**: Validate `Itinerary_placeId` format

### Debug Tips

- Enable Postman console to see request/response details
- Verify environment variables are set correctly
- Check server logs for detailed error messages
- Ensure test database is properly seeded

## Future Enhancements

Potential additions to the test suite:

- **Bulk Interest Management**: Adding multiple places at once
- **Interest Filtering**: Tests for retrieving interest lists
- **Interest Removal**: DELETE endpoint testing
- **Performance Tests**: Load testing with multiple concurrent requests
- **Edge Cases**: Extremely long place names, special characters