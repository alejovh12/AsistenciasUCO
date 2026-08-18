package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.TipoIdentificacionRepositoryEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertNotNull(resultado.get(0).getId());
        assertEquals(UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c"), resultado.get(0).getId());
        assertThrows(UnsupportedOperationException.class, () -> resultado.add(
                new TipoIdentificacionRepositoryEntity(
                        UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c"),
                        "XX",
                        "Temporal"
                )
        ));
    }
}
