package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.TipoIdentificacionRepositoryEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TipoIdentificacionRepositoryMockAdapterTest {

    @Test
    void consultar_tipos_identificacion_retorna_tres_registros() {
        final TipoIdentificacionRepositoryMockAdapter adapter = new TipoIdentificacionRepositoryMockAdapter();

        final List<TipoIdentificacionRepositoryEntity> resultado = adapter.consultarTiposIdentificacion();

        assertNotNull(resultado);
        assertEquals(3, resultado.size());
        assertEquals("CC", resultado.get(0).getTipoIdentificacion());
        assertEquals("PA", resultado.get(1).getTipoIdentificacion());
        assertEquals("TI", resultado.get(2).getTipoIdentificacion());
    }
}
