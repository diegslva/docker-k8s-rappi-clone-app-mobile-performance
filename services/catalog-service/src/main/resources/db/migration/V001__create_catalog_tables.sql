-- Lojas
CREATE TABLE stores (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(100) NOT NULL,
    owner_user_id   UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(30) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(255),
    logo_url        VARCHAR(500),
    banner_url      VARCHAR(500),
    street          VARCHAR(255) NOT NULL,
    number          VARCHAR(20) NOT NULL,
    complement      VARCHAR(100),
    neighborhood    VARCHAR(100) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(2) NOT NULL,
    zip_code        VARCHAR(10) NOT NULL,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    min_order_value DECIMAL(10,2) DEFAULT 0,
    avg_prep_time   INT DEFAULT 30,
    delivery_radius_km DECIMAL(5,2) DEFAULT 5.00,
    rating_avg      DECIMAL(3,2) DEFAULT 0,
    rating_count    INT DEFAULT 0,
    is_open         BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_store_slug_tenant UNIQUE (slug, tenant_id)
);

CREATE INDEX idx_stores_tenant ON stores (tenant_id);
CREATE INDEX idx_stores_category ON stores (category);
CREATE INDEX idx_stores_active ON stores (tenant_id, is_active);
CREATE INDEX idx_stores_owner ON stores (owner_user_id);

-- Horarios de funcionamento
CREATE TABLE store_hours (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id    UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    day_of_week INT NOT NULL,
    open_time   TIME NOT NULL,
    close_time  TIME NOT NULL,

    CONSTRAINT uq_store_hours UNIQUE (store_id, day_of_week)
);

-- Categorias de produtos dentro de uma loja
CREATE TABLE product_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id    UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    tenant_id   VARCHAR(100) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_category_slug_store UNIQUE (store_id, slug)
);

CREATE INDEX idx_product_categories_store ON product_categories (store_id);

-- Produtos
CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    category_id     UUID REFERENCES product_categories(id),
    tenant_id       VARCHAR(100) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2) NOT NULL,
    original_price  DECIMAL(10,2),
    image_url       VARCHAR(500),
    prep_time_min   INT,
    is_available    BOOLEAN NOT NULL DEFAULT TRUE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_product_slug_store UNIQUE (store_id, slug)
);

CREATE INDEX idx_products_store ON products (store_id);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_tenant ON products (tenant_id);
CREATE INDEX idx_products_available ON products (store_id, is_available, is_active);

-- Modificadores/add-ons de produtos (ex: tamanho, extras)
CREATE TABLE product_modifiers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    group_name      VARCHAR(100) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    price           DECIMAL(10,2) NOT NULL DEFAULT 0,
    is_required     BOOLEAN NOT NULL DEFAULT FALSE,
    max_selections  INT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_modifiers_product ON product_modifiers (product_id);
