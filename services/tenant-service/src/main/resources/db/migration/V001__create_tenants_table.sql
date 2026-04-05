-- Tenants: cada tenant = uma operacao local (cidade/metro)
CREATE TABLE tenants (
    id              VARCHAR(100) PRIMARY KEY,  -- ex: "londrina-pr", "sao-paulo-sp"
    name            VARCHAR(255) NOT NULL,
    state           VARCHAR(2) NOT NULL,       -- UF (PR, SP, CE...)
    country         VARCHAR(2) NOT NULL DEFAULT 'BR',
    timezone        VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    currency        VARCHAR(3) NOT NULL DEFAULT 'BRL',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    service_fee_pct DECIMAL(5,2) NOT NULL DEFAULT 10.00, -- taxa da plataforma (%)
    min_order_value DECIMAL(10,2) NOT NULL DEFAULT 15.00,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Zonas de entrega: poligonos PostGIS dentro de um tenant
CREATE TABLE zones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(100) NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    polygon         GEOMETRY(POLYGON, 4326) NOT NULL,
    base_delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 5.00,
    max_delivery_radius_km DECIMAL(5,2) NOT NULL DEFAULT 10.00,
    estimated_delivery_minutes INT NOT NULL DEFAULT 45,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_zone_slug_tenant UNIQUE (tenant_id, slug)
);

CREATE INDEX idx_zones_tenant ON zones (tenant_id);
CREATE INDEX idx_zones_polygon ON zones USING GIST (polygon);

-- Micro-regioes: bairros/clusters dentro de uma zona
CREATE TABLE micro_regions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id         UUID NOT NULL REFERENCES zones(id),
    tenant_id       VARCHAR(100) NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    polygon         GEOMETRY(POLYGON, 4326) NOT NULL,
    avg_ticket      DECIMAL(10,2),            -- ticket medio (calculado por batch ML)
    median_income   DECIMAL(10,2),            -- renda mediana estimada
    price_elasticity DECIMAL(3,2) DEFAULT 0.5, -- 0.0 insensivel, 1.0 muito sensivel
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_micro_region_slug_zone UNIQUE (zone_id, slug)
);

CREATE INDEX idx_micro_regions_zone ON micro_regions (zone_id);
CREATE INDEX idx_micro_regions_tenant ON micro_regions (tenant_id);
CREATE INDEX idx_micro_regions_polygon ON micro_regions USING GIST (polygon);
