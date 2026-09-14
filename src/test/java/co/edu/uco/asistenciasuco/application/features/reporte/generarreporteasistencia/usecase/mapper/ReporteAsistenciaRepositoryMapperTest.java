package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporteAsistenciaRepositoryMapperTest {

    @Test
    void toEntity_mapea_todos_los_campos() {
        final ReporteAsistenciaRow row = new ReporteAsistenciaRow(
                "G1", "Grupo 1", 3, "Sesion 3", LocalDateTime.of(2026, 1, 20, 8, 0),
                LocalDateTime.of(2026, 1, 20, 10, 0), "123456789", "Ana Perez", "ana@uco.edu.co", true, null);

        final ReporteAsistenciaEntity entity = ReporteAsistenciaRepositoryMapper.toEntity(row);

        assertEquals("G1", entity.codigoGrupo());
        assertEquals("Ana Perez", entity.nombreEstudiante());
        assertTrue(entity.asistio());
    }
}
