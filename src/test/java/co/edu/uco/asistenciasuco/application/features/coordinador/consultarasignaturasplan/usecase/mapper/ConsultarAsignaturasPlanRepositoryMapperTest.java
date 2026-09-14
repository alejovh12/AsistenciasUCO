package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarAsignaturasPlanRepositoryMapperTest {

    @Test
    void toDomain_mapea_todos_los_campos_de_la_proyeccion() {
        final UUID id = UUID.randomUUID();
        final AsignaturaProjection projection = new AsignaturaProjection(
                id, "COD1", "Calculo", 3, UUID.randomUUID(), "Area",
                UUID.randomUUID(), "Componente", UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Programa", "S1", true, "Activa");

        final AsignaturaDomain domain = ConsultarAsignaturasPlanRepositoryMapper.toDomain(projection);

        assertEquals(id, domain.id());
        assertEquals("COD1", domain.codigo());
        assertTrue(domain.estaActivaAsignatura());
    }
}
