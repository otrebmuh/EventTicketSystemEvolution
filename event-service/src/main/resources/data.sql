-- Insert default event categories if they don't exist
INSERT INTO event_categories (id, name, description, display_order, is_active) 
SELECT * FROM (VALUES 
    (gen_random_uuid(), 'Music', 'Concerts, festivals, and musical performances', 1, true),
    (gen_random_uuid(), 'Sports', 'Sporting events, games, and competitions', 2, true),
    (gen_random_uuid(), 'Theater', 'Plays, musicals, and theatrical performances', 3, true),
    (gen_random_uuid(), 'Comedy', 'Stand-up comedy and comedy shows', 4, true),
    (gen_random_uuid(), 'Arts & Culture', 'Art exhibitions, cultural events, and museums', 5, true),
    (gen_random_uuid(), 'Food & Drink', 'Food festivals, wine tastings, and culinary events', 6, true),
    (gen_random_uuid(), 'Business', 'Conferences, seminars, and networking events', 7, true),
    (gen_random_uuid(), 'Technology', 'Tech conferences, workshops, and meetups', 8, true),
    (gen_random_uuid(), 'Health & Wellness', 'Fitness classes, wellness workshops, and health events', 9, true),
    (gen_random_uuid(), 'Family', 'Family-friendly events and activities', 10, true),
    (gen_random_uuid(), 'Education', 'Educational workshops, classes, and seminars', 11, true),
    (gen_random_uuid(), 'Other', 'Miscellaneous events and activities', 12, true)
) AS v(id, name, description, display_order, is_active)
WHERE NOT EXISTS (SELECT 1 FROM event_categories WHERE name = v.name);