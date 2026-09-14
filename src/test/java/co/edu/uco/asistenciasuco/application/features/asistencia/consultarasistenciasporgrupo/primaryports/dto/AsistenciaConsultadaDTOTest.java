package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AsistenciaConsultadaDTOTest {

    @Test
    void constructor_completo_asigna_todos_los_campos() {
        final UUID asistencia = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();

        final AsistenciaConsultadaDTO dto = new AsistenciaConsultadaDTO(asistencia, estudiante, grupo, sesion, true, "A tiempo");

        assertEquals(asistencia, dto.getAsistencia());
        assertEquals(estudiante, dto.getEstudiante());
        assertEquals(grupo, dto.getGrupo());
        assertEquals(sesion, dto.getSesion());
        assertEquals(true, dto.getPresente());
        assertEquals("A tiempo", dto.getObservacion());
    }

    @Test
    void constructor_vacio_y_setters_permiten_construccion_incremental() {
        final AsistenciaConsultadaDTO dto = new AsistenciaConsultadaDTO();
        final UUID asistencia = UUID.randomUUID();

        dto.setAsistencia(asistencia);
        dto.setEstudiante(null);
        dto.setGrupo(null);
        dto.setSesion(null);
        dto.setPresente(false);
        dto.setObservacion(null);

        assertEquals(asistencia, dto.getAsistencia());
        assertNull(dto.getEstudiante());
        assertEquals(false, dto.getPresente());
        assertNull(dto.getObservacion());
    }
}
