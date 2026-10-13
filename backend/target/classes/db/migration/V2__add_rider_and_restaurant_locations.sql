-- Migration V2: Add real-time GPS locations and activity timestamps for delivery assignment
ALTER TABLE delivery_men ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE delivery_men ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE delivery_men ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP;
ALTER TABLE delivery_men ALTER COLUMN is_available SET DEFAULT false;

ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Populate seed restaurants with Bengaluru coordinates
UPDATE restaurants SET latitude = 12.9756, longitude = 77.6066 WHERE id = 1 AND latitude IS NULL;
UPDATE restaurants SET latitude = 12.9716, longitude = 77.6085 WHERE id = 2 AND latitude IS NULL;
UPDATE restaurants SET latitude = 12.9784, longitude = 77.6408 WHERE id = 3 AND latitude IS NULL;

-- Mark seeded demo delivery accounts (David Vance, Elena Rostova) as offline by default so they aren't phantom assigned!
UPDATE delivery_men SET is_available = false WHERE email IN ('rider1@foodnest.com', 'rider2@foodnest.com');
