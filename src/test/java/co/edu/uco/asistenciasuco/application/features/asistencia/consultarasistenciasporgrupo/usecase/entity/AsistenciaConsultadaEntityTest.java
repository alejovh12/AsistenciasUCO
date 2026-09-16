package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsistenciaConsultadaEntityTest {

    private static final UUID ASISTENCIA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();

    @Test
    void constructor_con_todos_los_identificadores_validos_crea_entidad() {
        final AsistenciaConsultadaEntity entity = new AsistenciaConsultadaEntity(
                ASISTENCIA, ESTUDIANTE, GRUPO, SESION, true, "ex", "Llego a tiempo");

        assertEquals(ASISTENCIA, entity.getAsistencia());
        assertEquals(ESTUDIANTE, entity.getEstudiante());
        assertEquals(GRUPO, entity.getGrupo());
        assertEquals(SESION, entity.getSesion());
        assertTrue(entity.isPresente());
        assertEquals("EX", entity.getEstado());
        assertEquals("Llego a tiempo", entity.getObservacion());
    }

    @Test
    void constructor_rechaza_asistencia_nula() {
        assertThrows(ValidationException.class,
                () -> new AsistenciaConsultadaEntity(null, ESTUDIANTE, GRUPO, SESION, true, "EX", null));
    }

    @Test
    void constructor_rechaza_estudiante_nulo() {
        assertThrows(ValidationException.class,
                () -> new AsistenciaConsultadaEntity(ASISTENCIA, null, GRUPO, SESION, true, "EX", null));
    }

    @Test
    void constructor_rechaza_grupo_nulo() {
        assertThrows(ValidationException.class,
                () -> new AsistenciaConsultadaEntity(ASISTENCIA, ESTUDIANTE, null, SESION, true, "EX", null));
    }

    @Test
    void constructor_rechaza_sesion_nula() {
        assertThrows(ValidationException.class,
                () -> new AsistenciaConsultadaEntity(ASISTENCIA, ESTUDIANTE, GRUPO, null, true, "EX", null));
    }

    @Test
    void constructor_con_observacion_en_blanco_normaliza_a_nulo() {
        final AsistenciaConsultadaEntity entity = new AsistenciaConsultadaEntity(
                ASISTENCIA, ESTUDIANTE, GRUPO, SESION, false, "SJC", "   ");

        assertNull(entity.getObservacion());
    }

    @Test
    void constructor_con_observacion_muy_corta_lanza_validationException() {
        assertThrows(ValidationException.class,
                () -> new AsistenciaConsultadaEntity(ASISTENCIA, ESTUDIANTE, GRUPO, SESION, false, "SJC", "hi"));
    }

    @Test
    void constructor_normaliza_estado_a_mayusculas_y_permite_excusa_sin_reconstruirse_a_sjc() {
        final AsistenciaConsultadaEntity entity = new AsistenciaConsultadaEntity(
                ASISTENCIA, ESTUDIANTE, GRUPO, SESION, false, " ex ", null);

        assertEquals("EX", entity.getEstado());
    }
}
