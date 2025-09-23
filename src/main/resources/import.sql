
-- This file is automatically executed by Hibernate after schema creation
-- Only works with ddl-auto: create, create-drop

-- Insert test users (passwords are BCrypt encoded)
-- Note: created_at and updated_at are auto-populated by JPA auditing
INSERT INTO users (role, email, username, password) VALUES 
('USER', 'f36meng@gmail.com', 'f36meng', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.'),
('ADMIN', 'fmeng48@gmail.com', 'fmeng48', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.'),
('USER', 'testuser@example.com', 'testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.');

-- Insert sample places with JSONB data
-- Note: created_at and updated_at are auto-populated by JPA auditing
INSERT INTO places (external_place_id, name, address, latitude, longitude, website, phone, image_url, description, opening_hours, contact_info, metadata, raw_data, source) VALUES 
('ChIJIQBpAG2ahYAR_6128GcTUEo', 'Golden Gate Bridge', 'Golden Gate Bridge, San Francisco, CA, USA', 37.8199, -122.4783, 'https://www.goldengate.org', '(415) 921-5858', 'https://example.com/golden-gate.jpg', 'Famous suspension bridge spanning the Golden Gate strait', 
 '{"monday": {"open": "00:00", "close": "23:59"}, "tuesday": {"open": "00:00", "close": "23:59"}, "wednesday": {"open": "00:00", "close": "23:59"}, "thursday": {"open": "00:00", "close": "23:59"}, "friday": {"open": "00:00", "close": "23:59"}, "saturday": {"open": "00:00", "close": "23:59"}, "sunday": {"open": "00:00", "close": "23:59"}}',
 '{"email": "info@goldengate.org", "phone": "(415) 921-5858"}',
 '{"category": "landmark", "rating": 4.8, "price_level": 0, "accessibility": true}',
 '{"place_types": ["tourist_attraction", "point_of_interest"], "photos": ["photo_ref_1", "photo_ref_2"]}',
 'google_places'),

('ChIJ85QQqtr8j4ARTk2+Zqn6+Ao', 'Alcatraz Island', 'Alcatraz Island, San Francisco, CA, USA', 37.8267, -122.4230, 'https://www.nps.gov/alca', '(415) 561-4900', 'https://example.com/alcatraz.jpg', 'Historic former federal prison on an island in San Francisco Bay',
 '{"monday": {"open": "09:00", "close": "18:00"}, "tuesday": {"open": "09:00", "close": "18:00"}, "wednesday": {"open": "09:00", "close": "18:00"}, "thursday": {"open": "09:00", "close": "18:00"}, "friday": {"open": "09:00", "close": "18:00"}, "saturday": {"open": "09:00", "close": "18:00"}, "sunday": {"open": "09:00", "close": "18:00"}}',
 '{"email": "alca_information@nps.gov", "phone": "(415) 561-4900"}',
 '{"category": "museum", "rating": 4.6, "price_level": 3, "accessibility": false}',
 '{"place_types": ["museum", "tourist_attraction"], "requires_booking": true}',
 'google_places'),

('ChIJIQBpAG2ahYAR_6128GcTUEr', 'Fishermans Wharf', 'Pier 39, San Francisco, CA 94133, USA', 37.8087, -122.4098, 'https://www.pier39.com', '(415) 705-5500', 'https://example.com/pier39.jpg', 'Popular waterfront marketplace and tourist attraction',
 '{"monday": {"open": "10:00", "close": "21:00"}, "tuesday": {"open": "10:00", "close": "21:00"}, "wednesday": {"open": "10:00", "close": "21:00"}, "thursday": {"open": "10:00", "close": "21:00"}, "friday": {"open": "10:00", "close": "22:00"}, "saturday": {"open": "10:00", "close": "22:00"}, "sunday": {"open": "10:00", "close": "21:00"}}',
 '{"email": "info@pier39.com", "phone": "(415) 705-5500"}',
 '{"category": "shopping", "rating": 4.2, "price_level": 2, "accessibility": true}',
 '{"place_types": ["shopping_mall", "tourist_attraction"], "parking_available": true}',
 'google_places'),

('ChIJ-Y7ASBK4j4AR4_MmxOcvR88', 'Lombard Street', '1071 Lombard St, San Francisco, CA 94133, USA', 37.8021, -122.4187, null, null, 'https://example.com/lombard.jpg', 'The most crooked street in the world with eight hairpin turns',
 '{"monday": {"open": "00:00", "close": "23:59"}, "tuesday": {"open": "00:00", "close": "23:59"}, "wednesday": {"open": "00:00", "close": "23:59"}, "thursday": {"open": "00:00", "close": "23:59"}, "friday": {"open": "00:00", "close": "23:59"}, "saturday": {"open": "00:00", "close": "23:59"}, "sunday": {"open": "00:00", "close": "23:59"}}',
 '{"contact_method": "none"}',
 '{"category": "street", "rating": 4.3, "price_level": 0, "accessibility": false}',
 '{"place_types": ["tourist_attraction", "route"], "driving_restrictions": true}',
 'google_places'),

('ChIJ-1bbe5aHhYARWdCR_Vda-qY', 'Chinatown', '24 Grant Ave, San Francisco, CA 94108, USA', 37.7941, -122.4078, 'https://www.sanfranciscochinatown.com', null, 'https://example.com/chinatown.jpg', 'Historic neighborhood with authentic Chinese culture and cuisine',
 '{"monday": {"open": "06:00", "close": "22:00"}, "tuesday": {"open": "06:00", "close": "22:00"}, "wednesday": {"open": "06:00", "close": "22:00"}, "thursday": {"open": "06:00", "close": "22:00"}, "friday": {"open": "06:00", "close": "23:00"}, "saturday": {"open": "06:00", "close": "23:00"}, "sunday": {"open": "06:00", "close": "22:00"}}',
 '{"website": "https://www.sanfranciscochinatown.com"}',
 '{"category": "neighborhood", "rating": 4.4, "price_level": 1, "accessibility": true}',
 '{"place_types": ["neighborhood", "tourist_attraction"], "languages": ["en", "zh"]}',
 'google_places', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample itineraries with JSONB data
-- Note: created_at and updated_at are auto-populated by JPA auditing
INSERT INTO itineraries (user_id, destination_city, start_date, end_date, travel_mode, budget_in_cents, daily_start, daily_end, seeded_recommendations, ai_metadata) VALUES 
(1, 'San Francisco', '2024-03-15T09:00:00-07:00', '2024-03-17T18:00:00-07:00', 'WALKING', 50000, '09:00:00', '18:00:00', '{"interests": ["landmarks", "culture", "food"], "preferences": {"pace": "moderate", "group_size": 2}}', '{"ai_model": "gpt-4", "generated_at": "2024-03-10T10:00:00Z", "confidence_score": 0.85}'),

(2, 'Los Angeles', '2024-04-01T08:00:00-07:00', '2024-04-05T20:00:00-07:00', 'DRIVING', 150000, '08:00:00', '20:00:00', '{"interests": ["beaches", "entertainment", "shopping"], "preferences": {"pace": "fast", "group_size": 4}}', '{"ai_model": "gpt-4", "generated_at": "2024-03-25T14:30:00Z", "confidence_score": 0.92}'),

(3, 'New York', '2024-05-10T06:00:00-04:00', '2024-05-12T22:00:00-04:00', 'TRANSIT', 75000, '07:00:00', '22:00:00', '{"interests": ["museums", "theater", "parks"], "preferences": {"pace": "slow", "group_size": 1}}', '{"ai_model": "gpt-4", "generated_at": "2024-05-01T09:15:00Z", "confidence_score": 0.78}');

-- Note: Additional tables (planned_days, planned_stops, itinerary_places) will need their JPA entities created first