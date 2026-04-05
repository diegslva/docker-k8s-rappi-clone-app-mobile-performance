-- Onboarding de lojas (restaurantes, mercados, farmacias)
CREATE TABLE store_applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    owner_user_id       UUID NOT NULL,
    store_name          VARCHAR(255) NOT NULL,
    store_category      VARCHAR(30) NOT NULL,
    tax_id              VARCHAR(20) NOT NULL,
    tax_regime          VARCHAR(30) NOT NULL DEFAULT 'SIMPLES_NACIONAL',
    legal_name          VARCHAR(255) NOT NULL,
    phone               VARCHAR(20) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    street              VARCHAR(255) NOT NULL,
    number              VARCHAR(20) NOT NULL,
    complement          VARCHAR(100),
    neighborhood        VARCHAR(100) NOT NULL,
    city                VARCHAR(100) NOT NULL,
    state               VARCHAR(2) NOT NULL,
    zip_code            VARCHAR(10) NOT NULL,
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING_DOCUMENTS',
    rejection_reason    TEXT,
    reviewed_by         UUID,
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_store_app_tax_id_tenant UNIQUE (tax_id, tenant_id)
);

CREATE INDEX idx_store_app_tenant ON store_applications (tenant_id);
CREATE INDEX idx_store_app_status ON store_applications (status);
CREATE INDEX idx_store_app_owner ON store_applications (owner_user_id);

-- Documentos associados a uma aplicacao de loja
CREATE TABLE store_application_documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID NOT NULL REFERENCES store_applications(id) ON DELETE CASCADE,
    document_type       VARCHAR(30) NOT NULL,
    media_id            UUID NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_store_docs_app ON store_application_documents (application_id);

-- Onboarding de couriers (entregadores)
CREATE TABLE courier_applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    user_id             UUID NOT NULL,
    full_name           VARCHAR(200) NOT NULL,
    cpf                 VARCHAR(14) NOT NULL,
    phone               VARCHAR(20) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    date_of_birth       DATE NOT NULL,
    vehicle_type        VARCHAR(20) NOT NULL,
    vehicle_plate       VARCHAR(10),
    cnh_number          VARCHAR(20),
    cnh_category        VARCHAR(5),
    cnh_expiry          DATE,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING_DOCUMENTS',
    rejection_reason    TEXT,
    reviewed_by         UUID,
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_courier_app_cpf_tenant UNIQUE (cpf, tenant_id)
);

CREATE INDEX idx_courier_app_tenant ON courier_applications (tenant_id);
CREATE INDEX idx_courier_app_status ON courier_applications (status);
CREATE INDEX idx_courier_app_user ON courier_applications (user_id);

-- Documentos associados a uma aplicacao de courier
CREATE TABLE courier_application_documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID NOT NULL REFERENCES courier_applications(id) ON DELETE CASCADE,
    document_type       VARCHAR(30) NOT NULL,
    media_id            UUID NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_courier_docs_app ON courier_application_documents (application_id);
