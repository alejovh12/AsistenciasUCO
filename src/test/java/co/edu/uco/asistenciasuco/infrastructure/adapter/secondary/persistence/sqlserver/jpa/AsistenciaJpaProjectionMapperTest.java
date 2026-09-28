package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AsistenciaJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaQueryRow;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JPA-Q-004..007: mapping puro fila JPA -> proyeccion del puerto con la semantica JDBC. */
class AsistenciaJpaProjectionMapperTest {

    private static final UUID ASISTENCIA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();

    @Test
    void copia_los_uuid_sin_transformarlos_y_fija_observacion_vacia() {
        final AsistenciaRepositoryProjection projection = AsistenciaJpaProjectionMapper.toProjection(
                new AsistenciaQueryRow(ASISTENCIA, ESTUDIANTE, GRUPO, SESION, true, "SJC"));

        assertEquals(ASISTENCIA, projection.getAsistencia());
        assertEquals(ESTUDIANTE, projection.getEstudiante());
        assertEquals(GRUPO, projection.getGrupo());
        assertEquals(SESION, projection.getSesion());
        assertEquals("SJC", projection.getEstado());
        assertEquals("", projection.getObservacion());
    }

    @Test
    void presente_true_y_false_se_conservan() {
        assertTrue(AsistenciaJpaProjectionMapper.toProjection(
                new AsistenciaQueryRow(ASISTENCIA, ESTUDIANTE, GRUPO, SESION, true, "AN")).isPresente());
        assertFalse(AsistenciaJpaProjectionMapper.toProjection(
                new AsistenciaQueryRow(ASISTENCIA, ESTUDIANTE, GRUPO, SESION, false, "EX")).isPresente());
    }

    @Test
    void presente_null_equivale_a_result_set_get_boolean_y_es_false() {
        assertFalse(AsistenciaJpaProjectionMapper.toProjection(
                new AsistenciaQueryRow(ASISTENCIA, ESTUDIANTE, GRUPO, SESION, null, "AN")).isPresente());
    }

    @Test
    void estado_null_no_se_defaultea_a_an() {
        final AsistenciaRepositoryProjection projection = AsistenciaJpaProjectionMapper.toProjection(
                new AsistenciaQueryRow(ASISTENCIA, ESTUDIANTE, GRUPO, SESION, true, null));

        assertNull(projection.getEstado());
    }

    @Test
    void estado_desconocido_pasa_tal_cual_sin_enum() {
        assertEquals("XX", AsistenciaJpaProjectionMapper.toProjection(
                new AsistenciaQueryRow(ASISTENCIA, ESTUDIANTE, GRUPO, SESION, true, "XX")).getEstado());
    }
}
