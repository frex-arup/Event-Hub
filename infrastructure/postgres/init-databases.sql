-- Creates all microservice databases inside a single PostgreSQL instance.
-- Mounted as /docker-entrypoint-initdb.d/init-databases.sql

CREATE DATABASE identity_db;
CREATE DATABASE event_db;
CREATE DATABASE booking_db;
CREATE DATABASE payment_db;
CREATE DATABASE social_db;
CREATE DATABASE finance_db;
