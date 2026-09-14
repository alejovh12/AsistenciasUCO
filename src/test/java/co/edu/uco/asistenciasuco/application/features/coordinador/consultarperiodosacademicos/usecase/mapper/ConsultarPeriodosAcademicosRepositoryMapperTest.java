package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.domain.PeriodoAcademicoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultarPeriodosAcademicosRepositoryMapperTest {

    @Test
    void toDomain_mapea_todos_los_campos() {
        final UUID id = UUID.randomUUID();
        final PeriodoAcademicoProjection projection = new PeriodoAcademicoProjection(
                id, UUID.randomUUID(), "UCO", "2026-1", "2026-1",
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 5, 30), 2026);

        final PeriodoAcademicoDomain domain = ConsultarPeriodosAcademicosRepositoryMapper.toDomain(projection);

        assertEquals(id, domain.id());
        assertEquals("2026-1", domain.nombre());
        assertEquals(2026, domain.anio());
    }
}
