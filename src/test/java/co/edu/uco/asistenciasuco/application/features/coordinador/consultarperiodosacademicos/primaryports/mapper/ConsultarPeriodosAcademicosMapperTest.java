package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.domain.PeriodoAcademicoDomain;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultarPeriodosAcademicosMapperTest {

    @Test
    void toDTO_mapea_todos_los_campos() {
        final UUID id = UUID.randomUUID();
        final PeriodoAcademicoDomain domain = new PeriodoAcademicoDomain(
                id, UUID.randomUUID(), "UCO", "2026-1", "2026-1",
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 5, 30), 2026);

        final PeriodoAcademicoDTO dto = ConsultarPeriodosAcademicosMapper.toDTO(domain);

        assertEquals(id, dto.id());
        assertEquals("2026-1", dto.nombre());
        assertEquals(2026, dto.anio());
    }
}
