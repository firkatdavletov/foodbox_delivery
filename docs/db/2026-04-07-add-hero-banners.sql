-- Hero banners table
CREATE TABLE IF NOT EXISTS hero_banners (
    id                   UUID PRIMARY KEY,
    code                 VARCHAR(128)    NOT NULL,
    storefront_code      VARCHAR(128)    NOT NULL,
    placement            VARCHAR(32)     NOT NULL,
    status               VARCHAR(32)     NOT NULL,
    sort_order           INTEGER         NOT NULL,
    desktop_image_url    VARCHAR(1024)   NOT NULL,
    mobile_image_url     VARCHAR(1024),
    primary_action_url   VARCHAR(1024),
    secondary_action_url VARCHAR(1024),
    theme_variant        VARCHAR(32)     NOT NULL,
    text_alignment       VARCHAR(32)     NOT NULL,
    starts_at            TIMESTAMPTZ,
    ends_at              TIMESTAMPTZ,
    published_at         TIMESTAMPTZ,
    version              BIGINT          NOT NULL DEFAULT 0,
    deleted_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ     NOT NULL,
    updated_at           TIMESTAMPTZ     NOT NULL,

    CONSTRAINT uk_hero_banners_storefront_code UNIQUE (storefront_code, code)
);

CREATE INDEX IF NOT EXISTS idx_hero_banners_storefront_code ON hero_banners(storefront_code);
CREATE INDEX IF NOT EXISTS idx_hero_banners_placement        ON hero_banners(placement);
CREATE INDEX IF NOT EXISTS idx_hero_banners_status           ON hero_banners(status);
CREATE INDEX IF NOT EXISTS idx_hero_banners_starts_at        ON hero_banners(starts_at);
CREATE INDEX IF NOT EXISTS idx_hero_banners_ends_at          ON hero_banners(ends_at);
CREATE INDEX IF NOT EXISTS idx_hero_banners_sort_order       ON hero_banners(sort_order);
CREATE INDEX IF NOT EXISTS idx_hero_banners_deleted_at       ON hero_banners(deleted_at);

-- Hero banner translations table
CREATE TABLE IF NOT EXISTS hero_banner_translations (
    id                     UUID PRIMARY KEY,
    banner_id              UUID         NOT NULL REFERENCES hero_banners(id),
    locale                 VARCHAR(16)  NOT NULL,
    title                  VARCHAR(512) NOT NULL,
    subtitle               VARCHAR(512),
    description            TEXT,
    desktop_image_alt      VARCHAR(512) NOT NULL,
    mobile_image_alt       VARCHAR(512),
    primary_action_label   VARCHAR(255),
    secondary_action_label VARCHAR(255),
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_hero_banner_translations_banner_locale UNIQUE (banner_id, locale)
);
