package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity;

import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SesionConsultadaEntityTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void construye_entidad_valida_y_normaliza_textos() {
        final SesionConsultadaEntity entity = new SesionConsultadaEntity(
                SESION,
                GRUPO,
                "  Tema valido  ",
                "  Descripcion valida de prueba  ",
                true,
                "  Cierre correcto  "
        );

        assertEquals("Tema valido", entity.getTema());
        assertEquals("Descripcion valida de prueba", entity.getDescripcion());
        assertEquals("Cierre correcto", entity.getObservacionCierre());
    }

    @Test
    void permite_descripcion_y_observacion_cierre_vacias() {
        final SesionConsultadaEntity entity = new SesionConsultadaEntity(
                SESION,
                GRUPO,
                "Tema valido",
                "   ",
                false,
                "   "
        );

        assertNull(entity.getDescripcion());
        assertNull(entity.getObservacionCierre());
    }

    @Test
    void rechaza_identificadores_y_textos_invalidos() {
        assertThrows(ValidationException.class, () -> new SesionConsultadaEntity(null, GRUPO, "Tema valido", null, false, null));
        assertThrows(ValidationException.class, () -> new SesionConsultadaEntity(SESION, null, "Tema valido", null, false, null));
        assertThrows(ValidationException.class, () -> new SesionConsultadaEntity(SESION, GRUPO, null, null, false, null));
        assertThrows(ValidationException.class, () -> new SesionConsultadaEntity(SESION, GRUPO, "abcd", null, false, null));
        assertThrows(ValidationException.class, () -> new SesionConsultadaEntity(SESION, GRUPO, "Tema valido", "corta", false, null));
        assertThrows(ValidationException.class, () -> new SesionConsultadaEntity(SESION, GRUPO, "Tema valido", null, false, "abcd"));
    }
}
