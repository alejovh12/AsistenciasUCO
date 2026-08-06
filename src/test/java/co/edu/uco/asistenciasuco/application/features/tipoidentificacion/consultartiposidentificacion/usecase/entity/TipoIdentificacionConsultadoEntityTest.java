package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TipoIdentificacionConsultadoEntityTest {

    @Test
    void constructor_aplica_invariantes_y_normaliza_textos() {
        final TipoIdentificacionConsultadoEntity entity = new TipoIdentificacionConsultadoEntity(
                UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c"),
                " CC ",
                " Pasaporte "
        );

        assertEquals("CC", entity.getTipoIdentificacion());
        assertEquals("Pasaporte", entity.getNombre());
    }

    @Test
    void constructor_falla_si_tipo_identificacion_supera_la_longitud_maxima() {
        assertThrows(IllegalArgumentException.class, () -> new TipoIdentificacionConsultadoEntity(
                UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c"),
                "ABCDE1",
                "Pasaporte"
        ));
    }
}
