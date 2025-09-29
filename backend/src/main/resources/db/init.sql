-- Database initialization script for Docker
-- This script runs only once when the PostgreSQL container is first created

-- Create the database (if using docker-entrypoint-initdb.d, this might not be needed)
-- The database is already created by POSTGRES_DB environment variable

-- Set timezone
SET timezone = 'UTC';

-- Log initialization
SELECT 'Database initialization started' AS message;