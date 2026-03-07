CREATE TABLE IF NOT EXISTS images (
    id SERIAL PRIMARY KEY,
    storage_key VARCHAR(255) NOT NULL,
    variant VARCHAR(32) NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    size_bytes BIGINT NOT NULL,
    mime VARCHAR(255) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_images_variant CHECK (variant IN ('original', 'thumb', 'card'))
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_images_storage_key_unique ON images (storage_key);
CREATE INDEX IF NOT EXISTS idx_images_variant ON images (variant);
CREATE INDEX IF NOT EXISTS idx_images_is_primary ON images (is_primary);
CREATE INDEX IF NOT EXISTS idx_images_sort_order ON images (sort_order);
