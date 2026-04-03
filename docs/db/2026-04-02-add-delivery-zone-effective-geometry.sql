CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE delivery_zones
    ADD COLUMN IF NOT EXISTS effective_geometry geometry(MultiPolygon,4326);

ALTER TABLE delivery_zones
    ALTER COLUMN geometry
    TYPE geometry(MultiPolygon,4326)
    USING CASE
        WHEN geometry IS NULL THEN NULL
        ELSE ST_Multi(ST_SetSRID(geometry, 4326))::geometry(MultiPolygon,4326)
    END;

ALTER TABLE delivery_zones
    ALTER COLUMN effective_geometry
    TYPE geometry(MultiPolygon,4326)
    USING CASE
        WHEN effective_geometry IS NULL THEN NULL
        ELSE ST_Multi(ST_SetSRID(effective_geometry, 4326))::geometry(MultiPolygon,4326)
    END;

CREATE INDEX IF NOT EXISTS idx_delivery_zones_geometry_gist
    ON delivery_zones
    USING GIST (geometry);

CREATE INDEX IF NOT EXISTS idx_delivery_zones_effective_geometry_gist
    ON delivery_zones
    USING GIST (effective_geometry);

DO $$
DECLARE
    zone_record RECORD;
    claimed_geometry geometry;
    candidate_geometry geometry;
BEGIN
    UPDATE delivery_zones
    SET effective_geometry = NULL
    WHERE type <> 'POLYGON'
       OR is_active = false
       OR geometry IS NULL;

    claimed_geometry := NULL;

    FOR zone_record IN
        SELECT id, geometry
        FROM delivery_zones
        WHERE is_active = true
          AND type = 'POLYGON'
          AND geometry IS NOT NULL
        ORDER BY priority DESC, updated_at DESC, id ASC
    LOOP
        candidate_geometry := CASE
            WHEN claimed_geometry IS NULL THEN zone_record.geometry
            ELSE ST_Difference(zone_record.geometry, claimed_geometry)
        END;

        candidate_geometry := CASE
            WHEN candidate_geometry IS NULL THEN NULL
            WHEN ST_IsEmpty(candidate_geometry) THEN NULL
            ELSE ST_Multi(ST_CollectionExtract(ST_SetSRID(candidate_geometry, 4326), 3))
        END;

        UPDATE delivery_zones
        SET effective_geometry = CASE
            WHEN candidate_geometry IS NULL THEN NULL
            WHEN ST_IsEmpty(candidate_geometry) THEN NULL
            ELSE candidate_geometry::geometry(MultiPolygon,4326)
        END
        WHERE id = zone_record.id;

        claimed_geometry := CASE
            WHEN claimed_geometry IS NULL THEN zone_record.geometry
            ELSE ST_Union(claimed_geometry, zone_record.geometry)
        END;
    END LOOP;
END $$;
