CREATE TABLE media (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(100) NOT NULL,
    owner_id        UUID NOT NULL,
    owner_type      VARCHAR(30) NOT NULL,
    category        VARCHAR(30) NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    bucket          VARCHAR(100) NOT NULL,
    object_key      VARCHAR(500) NOT NULL UNIQUE,
    url             VARCHAR(1000) NOT NULL,
    thumbnail_key   VARCHAR(500),
    thumbnail_url   VARCHAR(1000),
    width           INT,
    height          INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_tenant ON media (tenant_id);
CREATE INDEX idx_media_owner ON media (owner_id, owner_type);
CREATE INDEX idx_media_category ON media (category);
