-- Event Service Database Initialization Script

-- Create database and user
CREATE DATABASE event_service;
CREATE USER event_user WITH ENCRYPTED PASSWORD 'event_password';
GRANT ALL PRIVILEGES ON DATABASE event_service TO event_user;

-- Connect to the event_service database
\c event_service;

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO event_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO event_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO event_user;

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Insert default event categories
INSERT INTO event_categories (id, name, description, display_order, is_active) VALUES
    (uuid_generate_v4(), 'Music', 'Concerts, festivals, and musical performances', 1, true),
    (uuid_generate_v4(), 'Sports', 'Sporting events, games, and competitions', 2, true),
    (uuid_generate_v4(), 'Theater', 'Plays, musicals, and theatrical performances', 3, true),
    (uuid_generate_v4(), 'Comedy', 'Stand-up comedy and comedy shows', 4, true),
    (uuid_generate_v4(), 'Arts & Culture', 'Art exhibitions, cultural events, and museums', 5, true),
    (uuid_generate_v4(), 'Food & Drink', 'Food festivals, wine tastings, and culinary events', 6, true),
    (uuid_generate_v4(), 'Business', 'Conferences, seminars, and networking events', 7, true),
    (uuid_generate_v4(), 'Technology', 'Tech conferences, workshops, and meetups', 8, true),
    (uuid_generate_v4(), 'Health & Wellness', 'Fitness classes, wellness workshops, and health events', 9, true),
    (uuid_generate_v4(), 'Family', 'Family-friendly events and activities', 10, true),
    (uuid_generate_v4(), 'Education', 'Educational workshops, classes, and seminars', 11, true),
    (uuid_generate_v4(), 'Other', 'Miscellaneous events and activities', 12, true)
ON CONFLICT (name) DO NOTHING;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_events_organizer_id ON events(organizer_id);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_events_category_id ON events(category_id);
CREATE INDEX IF NOT EXISTS idx_events_price_range ON events(min_price, max_price);

-- Create full-text search index
CREATE INDEX IF NOT EXISTS idx_events_search_text ON events USING GIN (to_tsvector('english', name || ' ' || COALESCE(description, '')));

-- Create geospatial index for venues
CREATE INDEX IF NOT EXISTS idx_venues_location ON venues USING GIST (point(longitude, latitude));
CREATE INDEX IF NOT EXISTS idx_venues_city ON venues(city);

-- Create index for venue name and city combination
CREATE INDEX IF NOT EXISTS idx_venues_name_city ON venues(name, city);

-- Create index for category active status
CREATE INDEX IF NOT EXISTS idx_categories_active_order ON event_categories(is_active, display_order);