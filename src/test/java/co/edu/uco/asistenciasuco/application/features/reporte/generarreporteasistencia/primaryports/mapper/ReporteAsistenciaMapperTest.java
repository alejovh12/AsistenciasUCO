package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.ReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporteAsistenciaMapperTest {

    @Test
    void toDTO_mapea_todos_los_campos() {
        final ReporteAsistenciaEntity entity = new ReporteAsistenciaEntity(
                "G1", "Grupo 1", 3, "Sesion 3", LocalDateTime.of(2026, 1, 20, 8, 0),
                LocalDateTime.of(2026, 1, 20, 10, 0), "123456789", "Ana Perez", "ana@uco.edu.co", true, null);

        final ReporteAsistenciaDTO dto = ReporteAsistenciaMapper.toDTO(entity);

        assertEquals("G1", dto.codigoGrupo());
        assertEquals("Ana Perez", dto.nombreEstudiante());
        assertTrue(dto.asistio());
    }
}
