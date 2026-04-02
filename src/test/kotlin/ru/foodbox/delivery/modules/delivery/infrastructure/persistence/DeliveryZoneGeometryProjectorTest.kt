package ru.foodbox.delivery.modules.delivery.infrastructure.persistence

import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Connection
import java.sql.DatabaseMetaData
import javax.sql.DataSource
import kotlin.test.assertTrue

class DeliveryZoneGeometryProjectorTest {

    @Test
    fun `rebuilds effective geometries in postgres using priority order and ST difference`() {
        val dataSource = postgresDataSource()
        val jdbcTemplate = mock(JdbcTemplate::class.java)
        val projector = DeliveryZoneGeometryProjector(dataSource, jdbcTemplate)
        val sqlCaptor = ArgumentCaptor.forClass(String::class.java)

        projector.rebuildEffectiveGeometries()

        verify(jdbcTemplate).execute(sqlCaptor.capture())
        assertTrue(sqlCaptor.value.contains("ST_Difference"))
        assertTrue(sqlCaptor.value.contains("effective_geometry"))
        assertTrue(sqlCaptor.value.contains("ORDER BY priority ASC"))
    }

    @Test
    fun `skips rebuild outside postgres`() {
        val dataSource = mock(DataSource::class.java)
        val connection = mock(Connection::class.java)
        val metaData = mock(DatabaseMetaData::class.java)
        val jdbcTemplate = mock(JdbcTemplate::class.java)
        `when`(dataSource.connection).thenReturn(connection)
        `when`(connection.metaData).thenReturn(metaData)
        `when`(metaData.databaseProductName).thenReturn("H2")
        val projector = DeliveryZoneGeometryProjector(dataSource, jdbcTemplate)

        projector.rebuildEffectiveGeometries()

        verifyNoInteractions(jdbcTemplate)
        verify(connection).close()
    }

    private fun postgresDataSource(): DataSource {
        val dataSource = mock(DataSource::class.java)
        val connection = mock(Connection::class.java)
        val metaData = mock(DatabaseMetaData::class.java)
        `when`(dataSource.connection).thenReturn(connection)
        `when`(connection.metaData).thenReturn(metaData)
        `when`(metaData.databaseProductName).thenReturn("PostgreSQL")
        return dataSource
    }
}
