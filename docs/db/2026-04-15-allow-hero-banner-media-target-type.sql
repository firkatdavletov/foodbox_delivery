-- Allow hero-banner images in media upload flow.
ALTER TABLE IF EXISTS media_images
    DROP CONSTRAINT IF EXISTS media_images_target_type_check;

ALTER TABLE IF EXISTS media_images
    ADD CONSTRAINT media_images_target_type_check
    CHECK (target_type IN ('PRODUCT', 'CATEGORY', 'VARIANT', 'HERO_BANNER'));
