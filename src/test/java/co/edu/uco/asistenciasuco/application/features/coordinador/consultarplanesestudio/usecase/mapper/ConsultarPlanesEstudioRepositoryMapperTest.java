package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarPlanesEstudioRepositoryMapperTest {

    @Test
    void toDomain_mapea_todos_los_campos() {
        final UUID id = UUID.randomUUID();
        final PlanEstudioProjection projection = new PlanEstudioProjection(
                id, UUID.randomUUID(), "Ingenieria", "INP-01", true, "Activo", null);

        final PlanEstudioDomain domain = ConsultarPlanesEstudioRepositoryMapper.toDomain(projection);

        assertEquals(id, domain.id());
        assertEquals("INP-01", domain.inp());
        assertTrue(domain.estaActivoPlanEstudio());
    }
}
