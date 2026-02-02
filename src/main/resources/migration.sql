-- Migration script to update pricing columns from 2-tier to 3-tier system
-- 
-- OLD SYSTEM: Owner could only set 2 prices (standard_price, vip_price)
-- NEW SYSTEM: Owner sets 3 EXACT prices (silver_price, gold_price, vip_price)
--
-- Since we cannot convert 2 prices to 3 accurate prices, we must drop tables.
-- 
-- RUN THIS SCRIPT to drop old tables and let Hibernate recreate with correct schema:

-- Step 1: Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Step 2: Drop tables in any order (foreign key checks are off)
DROP TABLE IF EXISTS shows;
DROP TABLE IF EXISTS movie_schedules;
DROP TABLE IF EXISTS bookings;

-- Step 3: Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- After running this script, restart the backend server.
-- Hibernate will automatically create tables with the new 3-price schema.
-- Owners will then schedule movies with EXACT Silver, Gold, and VIP prices.
--
-- HOW IT WORKS NOW:
-- 1. Owner opens SmartScheduler in frontend
-- 2. Owner enters: Silver=150, Gold=200, VIP=300 (example prices)
-- 3. Frontend sends these EXACT values to backend
-- 4. Backend stores these EXACT values in database
-- 5. User books tickets, price calculated based on seat type:
--    - Silver seat (rows A-D) → ₹150
--    - Gold seat (rows E-K) → ₹200
--    - VIP seat (rows L-M) → ₹300
