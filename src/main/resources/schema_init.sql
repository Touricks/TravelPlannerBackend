-- Drop tables in correct order (respecting foreign key constraints)
DROP TABLE IF EXISTS planned_stops CASCADE;
DROP TABLE IF EXISTS planned_days CASCADE;
DROP TABLE IF EXISTS itinerary_places CASCADE;
DROP TABLE IF EXISTS places CASCADE;
DROP TABLE IF EXISTS itineraries CASCADE;
DROP TABLE IF EXISTS ai_request_logs CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Create ENUM types
DROP TYPE IF EXISTS travel_mode CASCADE;
CREATE TYPE travel_mode AS ENUM ('DRIVING', 'TRANSIT', 'WALKING', 'BICYCLING');

-- 1. Users table (updated for JPA)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(20) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Places table (stores all PlaceDTO information)
CREATE TABLE places (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_place_id VARCHAR(255) UNIQUE, -- Google Place ID
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    
    -- Basic contact info
    website VARCHAR(500),
    phone VARCHAR(50),
    image_url TEXT,
    description TEXT,
    
    -- JSONB fields for complex structures
    opening_hours JSONB, -- Complete OpeningHoursDTO structure
    contact_info JSONB,  -- Extended contact information
    metadata JSONB,      -- Categories, ratings, price levels, etc.
    
    -- Data source tracking
    source VARCHAR(50), -- 'google', 'ai_generated', 'user_input'
    raw_data JSONB,     -- Original API response
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Itineraries table (with JSONB for AI recommendations)
CREATE TABLE itineraries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    destination_city VARCHAR(255) NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    travel_mode travel_mode,
    budget_in_cents INTEGER,
    daily_start TIME,
    daily_end TIME,
    
    -- Store initial AI recommendations
    seeded_recommendations JSONB, -- CreateItineraryResponse.seededRecommendations
    ai_metadata JSONB,            -- AI model info, token usage, etc.
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Itinerary-Place relationship (with recommendation info)
CREATE TABLE itinerary_places (
    itinerary_id UUID REFERENCES itineraries(id) ON DELETE CASCADE,
    place_id UUID REFERENCES places(id) ON DELETE CASCADE,
    
    -- Recommendation metadata
    recommendation_type VARCHAR(50) DEFAULT 'ai_seed', -- 'ai_seed', 'ai_search', 'user_add'
    recommendation_rank INTEGER,     -- AI recommendation order
    recommendation_score DECIMAL(3,2), -- AI recommendation score
    
    -- User interaction
    pinned BOOLEAN DEFAULT FALSE,
    user_note TEXT,
    viewed BOOLEAN DEFAULT FALSE,
    viewed_at TIMESTAMP,
    selected_for_plan BOOLEAN DEFAULT FALSE, -- Selected for planning
    
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (itinerary_id, place_id)
);

-- 5. Planned days (with JSONB for complete day plan)
CREATE TABLE planned_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id UUID REFERENCES itineraries(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    
    -- Statistics
    distance_m INTEGER,
    travel_time_s INTEGER,
    
    -- Complete day plan as JSONB
    day_plan JSONB, -- Complete PlannedDay structure
    
    -- Version control
    version INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(itinerary_id, date, version)
);

-- 6. Planned stops (relational structure with JSONB extensions)
CREATE TABLE planned_stops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planned_day_id UUID REFERENCES planned_days(id) ON DELETE CASCADE,
    place_id UUID REFERENCES places(id),
    stop_order INTEGER NOT NULL,
    
    -- Time planning
    arrival_local TIME,
    depart_local TIME,
    stay_minutes INTEGER,
    
    -- Extended information as JSONB
    stop_details JSONB, -- Transport info, notes, warnings, etc.
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. AI request logs (for auditing and optimization)
CREATE TABLE ai_request_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id UUID REFERENCES itineraries(id),
    request_type VARCHAR(50), -- 'create_itinerary', 'get_recommendations', 'plan_itinerary'
    request_data JSONB,
    response_data JSONB,
    ai_model VARCHAR(50),
    response_time_ms INTEGER,
    token_usage JSONB,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
-- Geographic indexes (requires PostGIS extension if needed)
CREATE INDEX idx_places_location ON places(latitude, longitude);

-- JSONB indexes
CREATE INDEX idx_places_opening_hours ON places USING GIN(opening_hours);
CREATE INDEX idx_places_metadata ON places USING GIN(metadata);
CREATE INDEX idx_itineraries_recommendations ON itineraries USING GIN(seeded_recommendations);
CREATE INDEX idx_planned_days_plan ON planned_days USING GIN(day_plan);
CREATE INDEX idx_stops_details ON planned_stops USING GIN(stop_details);

-- Business logic indexes
CREATE INDEX idx_itinerary_places_type ON itinerary_places(itinerary_id, recommendation_type);
CREATE INDEX idx_itinerary_places_selected ON itinerary_places(itinerary_id, selected_for_plan) 
WHERE selected_for_plan = TRUE;
CREATE INDEX idx_planned_days_active ON planned_days(itinerary_id, is_active) 
WHERE is_active = TRUE;
CREATE INDEX idx_planned_stops_order ON planned_stops(planned_day_id, stop_order);

-- Foreign key indexes
CREATE INDEX idx_itineraries_user ON itineraries(user_id);
CREATE INDEX idx_itineraries_dates ON itineraries(start_date, end_date);
CREATE INDEX idx_places_external_id ON places(external_place_id);
CREATE INDEX idx_ai_logs_itinerary ON ai_request_logs(itinerary_id, created_at);