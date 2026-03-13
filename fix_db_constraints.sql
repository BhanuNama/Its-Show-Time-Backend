-- ==========================================
-- OPTION 1: QUICK DELETE (Use this if you just want to delete the user RIGHT NOW)
-- ==========================================
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM users WHERE email = 'djtillu437@gmail.com';
SET FOREIGN_KEY_CHECKS = 1;


-- ==========================================
-- OPTION 2: PERMANENT FIX (Recommended)
-- ==========================================
-- This changes the database structure so that deleting a user AUTOMATICALLY 
-- deletes their venues, bookings, etc. in the future.

-- STEP 1: Run this query to generate DROP commands for existing constraints.
-- Copy the output of this query and run it.
SELECT CONCAT('ALTER TABLE ', TABLE_NAME, ' DROP FOREIGN KEY ', CONSTRAINT_NAME, ';') 
FROM information_schema.key_column_usage 
WHERE REFERENCED_TABLE_NAME = 'users' AND TABLE_SCHEMA = 'showtime' AND REFERENCED_COLUMN_NAME = 'id';

-- STEP 2: Execute the DROP commands you just generated.

-- STEP 3: Run these commands to add the new Cascading Constraints:
ALTER TABLE venues ADD CONSTRAINT fk_venues_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE events ADD CONSTRAINT fk_events_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE bookings ADD CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE wishlist ADD CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE ratings ADD CONSTRAINT fk_ratings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
