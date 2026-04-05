-- Cria databases separados por servico (database-per-service pattern)
-- Cada servico tem seu proprio schema, isolado dos demais

CREATE DATABASE rappiclone_identity;
CREATE DATABASE rappiclone_user_profile;
CREATE DATABASE rappiclone_catalog;
CREATE DATABASE rappiclone_cart;
CREATE DATABASE rappiclone_order;
CREATE DATABASE rappiclone_payment;
CREATE DATABASE rappiclone_courier;
CREATE DATABASE rappiclone_notification;
CREATE DATABASE rappiclone_rating;
CREATE DATABASE rappiclone_promotion;
CREATE DATABASE rappiclone_pricing;
CREATE DATABASE rappiclone_geolocation;
CREATE DATABASE rappiclone_media;
CREATE DATABASE rappiclone_chat;
CREATE DATABASE rappiclone_support;
CREATE DATABASE rappiclone_onboarding;
CREATE DATABASE rappiclone_fiscal;
CREATE DATABASE rappiclone_settlement;
CREATE DATABASE rappiclone_tenant;

-- Habilita PostGIS no database de geolocation
\c rappiclone_geolocation;
CREATE EXTENSION IF NOT EXISTS postgis;

-- Habilita PostGIS no database de tenant (zonas de entrega)
\c rappiclone_tenant;
CREATE EXTENSION IF NOT EXISTS postgis;
