package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.TipoIdentificacionRepositoryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TipoIdentificacionRepositorySqlServerAdapterTest {

    @Test
    void consultarTiposIdentificacion_usa_vista_columnas_explicitas_y_orden() {
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final TipoIdentificacionRepositorySqlServerAdapter adapter =
                new TipoIdentificacionRepositorySqlServerAdapter(sql -> {
                    sqlCapturado.set(sql);
                    return List.of();
                });

        final List<TipoIdentificacionRepositoryEntity> resultado = adapter.consultarTiposIdentificacion();
        final String sql = sqlCapturado.get();

        assertEquals(0, resultado.size());
        assertFalse(sql.toUpperCase().contains("SELECT *"));
        assertTrue(sql.contains("id"));
        assertTrue(sql.contains("tipoIdentificacion"));
        assertTrue(sql.contains("nombre"));
        assertTrue(sql.contains("FROM dbo.uv_tipo_identificacion"));
        assertTrue(sql.contains("ORDER BY tipoIdentificacion"));
    }

    @Test
    void consultarTiposIdentificacion_retorna_varias_filas_preservando_orden() {
        final TipoIdentificacionRepositorySqlServerAdapter adapter =
                new TipoIdentificacionRepositorySqlServerAdapter(sql -> List.of(
                        new TipoIdentificacionRepositoryEntity(
                                UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c"),
                                "CC",
                                "Cedula"
                        ),
                        new TipoIdentificacionRepositoryEntity(
                                UUID.fromString("796a6c0f-f9fd-4327-a322-efd4d90bb81d"),
                                "PA",
                                "Pasaporte"
                        )
                ));

        final List<TipoIdentificacionRepositoryEntity> resultado = adapter.consultarTiposIdentificacion();

        assertEquals(2, resultado.size());
        assertEquals("CC", resultado.get(0).getTipoIdentificacion());
        assertEquals("PA", resultado.get(1).getTipoIdentificacion());
    }

    @Test
    void consultarTiposIdentificacion_convierte_error_de_acceso_a_datos() {
        final TipoIdentificacionRepositorySqlServerAdapter adapter =
                new TipoIdentificacionRepositorySqlServerAdapter(sql -> {
                    throw new DataAccessResourceFailureException("fallo tecnico");
                });

        assertThrows(DatabaseOperationException.class, adapter::consultarTiposIdentificacion);
    }

    @Test
    void mapRow_mapea_uuid_y_textos() throws SQLException {
        final UUID id = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id")).thenReturn(id);
        when(resultSet.getString("tipoIdentificacion")).thenReturn("CC");
        when(resultSet.getString("nombre")).thenReturn("Cedula");

        final TipoIdentificacionRepositoryEntity entity =
                TipoIdentificacionRepositorySqlServerAdapter.mapRow(resultSet, 0);

        assertEquals(id, entity.getId());
        assertEquals("CC", entity.getTipoIdentificacion());
        assertEquals("Cedula", entity.getNombre());
    }

    @Test
    void mapRow_convierte_uuid_desde_texto() throws SQLException {
        final UUID id = UUID.fromString("796a6c0f-f9fd-4327-a322-efd4d90bb81d");
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id")).thenReturn(id.toString());
        when(resultSet.getString("tipoIdentificacion")).thenReturn("PA");
        when(resultSet.getString("nombre")).thenReturn("Pasaporte");

        final TipoIdentificacionRepositoryEntity entity =
                TipoIdentificacionRepositorySqlServerAdapter.mapRow(resultSet, 0);

        assertEquals(id, entity.getId());
    }
}
