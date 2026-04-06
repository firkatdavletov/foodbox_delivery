-- Add derivative image keys and processing status to media_images
ALTER TABLE media_images ADD COLUMN thumb_key VARCHAR(512);
ALTER TABLE media_images ADD COLUMN card_key VARCHAR(512);
ALTER TABLE media_images ADD COLUMN processing_error TEXT;

-- Create image processing jobs table
CREATE TABLE image_processing_jobs (
    id              UUID PRIMARY KEY,
    image_id        UUID NOT NULL UNIQUE REFERENCES media_images(id),
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 3,
    next_retry_at   TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_image_processing_jobs_poll
    ON image_processing_jobs (status, next_retry_at)
    WHERE status IN ('PENDING', 'RETRY');
