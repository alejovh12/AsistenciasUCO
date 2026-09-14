package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PlanEstudioDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.domain.PlanEstudioDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarPlanesEstudioMapperTest {

    @Test
    void toDTO_mapea_todos_los_campos() {
        final UUID id = UUID.randomUUID();
        final PlanEstudioDomain domain = new PlanEstudioDomain(
                id, UUID.randomUUID(), "Ingenieria", "INP-01", true, "Activo", null);

        final PlanEstudioDTO dto = ConsultarPlanesEstudioMapper.toDTO(domain);

        assertEquals(id, dto.id());
        assertEquals("INP-01", dto.inp());
        assertTrue(dto.estaActivoPlanEstudio());
    }
}
