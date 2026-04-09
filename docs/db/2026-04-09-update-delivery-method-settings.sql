DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'delivery_method_settings'
          AND column_name = 'is_enabled'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'delivery_method_settings'
          AND column_name = 'is_active'
    ) THEN
        ALTER TABLE delivery_method_settings
            RENAME COLUMN is_enabled TO is_active;
    END IF;
END $$;

ALTER TABLE IF EXISTS delivery_method_settings
    ADD COLUMN IF NOT EXISTS title varchar(255);

ALTER TABLE IF EXISTS delivery_method_settings
    ADD COLUMN IF NOT EXISTS description text;

UPDATE delivery_method_settings
SET title = CASE method
    WHEN 'PICKUP' THEN 'Самовывоз'
    WHEN 'COURIER' THEN 'Доставка'
    WHEN 'YANDEX_PICKUP_POINT' THEN 'Доставка в ПВЗ Яндекс Маркет'
    WHEN 'CUSTOM_DELIVERY_ADDRESS' THEN 'Доставка по согласованию'
    ELSE method
END
WHERE title IS NULL OR btrim(title) = '';

UPDATE delivery_method_settings
SET description = CASE method
    WHEN 'PICKUP' THEN 'Заберите заказ в пункте самовывоза'
    WHEN 'COURIER' THEN 'Курьер доставит заказ по указанному адресу'
    WHEN 'YANDEX_PICKUP_POINT' THEN 'Получение заказа в пункте выдачи Яндекс Маркета'
    WHEN 'CUSTOM_DELIVERY_ADDRESS' THEN 'Адрес и условия доставки согласовываются отдельно после оформления заказа'
    ELSE description
END
WHERE description IS NULL;

ALTER TABLE IF EXISTS delivery_method_settings
    ALTER COLUMN title SET NOT NULL;
