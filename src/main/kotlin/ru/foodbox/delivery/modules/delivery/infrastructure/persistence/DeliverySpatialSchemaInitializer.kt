package ru.foodbox.delivery.modules.delivery.infrastructure.persistence

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class DeliverySpatialSchemaInitializer(
    private val dataSource: DataSource,
    private val jdbcTemplate: JdbcTemplate,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (!isPostgreSql()) {
            return
        }

        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis")
        jdbcTemplate.execute(
            """
            ALTER TABLE delivery_zones
            ALTER COLUMN geometry
            TYPE geometry(MultiPolygon,4326)
            USING CASE
                WHEN geometry IS NULL THEN NULL
                ELSE ST_Multi(ST_SetSRID(geometry, 4326))::geometry(MultiPolygon,4326)
            END
            """.trimIndent()
        )
        jdbcTemplate.execute(
            """
            CREATE INDEX IF NOT EXISTS idx_delivery_zones_geometry_gist
            ON delivery_zones
            USING GIST (geometry)
            """.trimIndent()
        )
    }

    private fun isPostgreSql(): Boolean {
        return dataSource.connection.use { connection ->
            connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        }
    }
}
