ALTER TABLE delivery_tariffs
    ADD COLUMN IF NOT EXISTS delivery_minutes INTEGER;

UPDATE delivery_tariffs
SET delivery_minutes = estimated_days * 1440
WHERE delivery_minutes IS NULL
  AND estimated_days > 0;

ALTER TABLE cart_delivery_drafts
    ADD COLUMN IF NOT EXISTS quote_estimates_minutes INTEGER;

UPDATE cart_delivery_drafts
SET quote_estimates_minutes = quote_estimated_days * 1440
WHERE quote_estimates_minutes IS NULL
  AND quote_estimated_days > 0;

ALTER TABLE order_delivery_snapshots
    ADD COLUMN IF NOT EXISTS estimates_minutes INTEGER;

UPDATE order_delivery_snapshots
SET estimates_minutes = estimated_days * 1440
WHERE estimates_minutes IS NULL
  AND estimated_days > 0;
