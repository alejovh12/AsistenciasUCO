package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TipoIdentificacionRepositoryEntityTest {

    @Test
    void constructor_solo_transporta_los_datos_sin_validaciones_de_dominio() {
        final TipoIdentificacionRepositoryEntity entity = new TipoIdentificacionRepositoryEntity(
                null,
                " CC ",
                null
        );

        assertNull(entity.getId());
        assertEquals(" CC ", entity.getTipoIdentificacion());
        assertNull(entity.getNombre());
    }
}
